package com.stu.huodongbm;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.stu.huodongbm.entity.Activity;
import com.stu.huodongbm.entity.Registration;
import com.stu.huodongbm.entity.User;
import com.stu.huodongbm.service.ActivityService;
import com.stu.huodongbm.service.RegistrationService;
import com.stu.huodongbm.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 最小集成测试套件：覆盖五轮安全/逻辑修复（H1-H4、M1-M5、L1-L9、CSRF、N1-N8）的关键回归点。
 *
 * 运行前提：本地 MySQL 在线且已建库（application.yml 默认数据源）。
 * 数据策略：类级 @Transactional —— 每个用例的写入在结束后自动回滚，不污染开发库；
 *          限速计数（LoginAttemptService）为内存态，用例使用随机用户名避免相互干扰。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class HuodongbmIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserService userService;
    @Autowired private ActivityService activityService;
    @Autowired private RegistrationService registrationService;

    private static final String CSRF = "it-csrf-token";

    // ================= 夹具 =================

    private User newUser(String role) {
        User u = new User();
        u.setUsername("it_" + UUID.randomUUID().toString().substring(0, 8));
        u.setPassword("123456");
        u.setRealName("集成测试用户");
        u.setRole(role);
        userService.save(u);
        return u;
    }

    private Activity newActivity(String title, int max) {
        Activity a = new Activity();
        a.setTitle(title);
        a.setMaxParticipants(max);
        a.setCurrentParticipants(0);
        a.setStatus("published");
        a.setStartTime(new Date(System.currentTimeMillis() + 86400000L));
        a.setEndTime(new Date(System.currentTimeMillis() + 2 * 86400000L));
        activityService.save(a);
        return a;
    }

    private Registration newRegistration(User u, Activity a, String status) {
        Registration r = new Registration();
        r.setUserId(u.getId());
        r.setActivityId(a.getId());
        r.setStatus(status);
        registrationService.save(r);
        return r;
    }

    /** 已登录会话（含 CSRF token） */
    private MockHttpSession sessionOf(User u) {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute("user", u);
        s.setAttribute("csrfToken", CSRF);
        return s;
    }

    /** 匿名会话（仅含 CSRF token，用于登录接口） */
    private MockHttpSession anonymousSession() {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute("csrfToken", CSRF);
        return s;
    }

    private String body(MvcResult r) throws Exception {
        return r.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    // ================= H4/N8：登录、惰性哈希、会话轮换 =================

    @Test
    void 登录成功_惰性哈希升级_会话轮换() throws Exception {
        User u = newUser("student");
        MockHttpSession old = anonymousSession();

        MvcResult r = mockMvc.perform(post("/login").session(old)
                .param("username", u.getUsername())
                .param("password", "123456")
                .param("_csrf", CSRF)).andReturn();

        assertEquals("/home", r.getResponse().getRedirectedUrl());
        assertTrue(old.isInvalid(), "登录成功后旧会话应失效（会话固定防护 N8）");
        User after = userService.getById(u.getId());
        assertEquals(60, after.getPassword().length(), "BCrypt 哈希应为60位");
        assertTrue(after.getPassword().startsWith("$2"), "明文密码应惰性升级为哈希");
    }

    @Test
    void 登录密码错误_返回友好提示() throws Exception {
        User u = newUser("student");
        MvcResult r = mockMvc.perform(post("/login").session(anonymousSession())
                .param("username", u.getUsername())
                .param("password", "wrong-pass")
                .param("_csrf", CSRF)).andReturn();

        assertNull(r.getResponse().getRedirectedUrl(), "失败不应重定向");
        assertTrue(body(r).contains("用户名或密码错误"));
    }

    // ================= N7：暴力破解锁定 =================

    @Test
    void 连续5次失败后第6次被锁定() throws Exception {
        String fake = "it_probe_" + UUID.randomUUID().toString().substring(0, 8);
        MockHttpSession s = anonymousSession();
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/login").session(s)
                    .param("username", fake).param("password", "x").param("_csrf", CSRF));
        }
        MvcResult r = mockMvc.perform(post("/login").session(s)
                .param("username", fake).param("password", "x").param("_csrf", CSRF)).andReturn();
        assertTrue(body(r).contains("失败次数过多"), "第6次应触发锁定提示");
    }

    // ================= N6：退出仅 POST =================

    @Test
    void 退出仅接受POST并使会话失效() throws Exception {
        MockHttpSession s = sessionOf(newUser("student"));

        MvcResult g = mockMvc.perform(get("/logout").session(s)).andReturn();
        assertEquals(405, g.getResponse().getStatus(), "GET /logout 应 405");

        MvcResult p = mockMvc.perform(post("/logout").session(s).param("_csrf", CSRF)).andReturn();
        assertEquals("/login", p.getResponse().getRedirectedUrl());
        assertTrue(s.isInvalid(), "退出后会话应失效");
    }

    // ================= H1/H3：越权拦截 =================

    @Test
    void 学生访问管理后台与审批接口被拦截() throws Exception {
        User stu = newUser("student");
        User other = newUser("student");
        Activity a = newActivity("IT鉴权活动", 10);
        Registration reg = newRegistration(other, a, "registered");
        MockHttpSession s = sessionOf(stu);

        MvcResult r1 = mockMvc.perform(get("/index").session(s)).andReturn();
        assertEquals("/activity/list", r1.getResponse().getRedirectedUrl(), "学生访问/index应被拦截(H3)");

        MvcResult r2 = mockMvc.perform(post("/registration/approve").session(s)
                .param("id", String.valueOf(reg.getId())).param("_csrf", CSRF)).andReturn();
        assertEquals("/activity/list", r2.getResponse().getRedirectedUrl(), "学生提交审批应被拦截(H1)");
        assertEquals("registered", registrationService.getById(reg.getId()).getStatus(), "审批状态不应变化");
    }

    @Test
    void 匿名访问受保护页跳转登录() throws Exception {
        MvcResult r = mockMvc.perform(get("/home")).andReturn();
        assertEquals("/login", r.getResponse().getRedirectedUrl());
    }

    @Test
    void 管理员访问数据总览正常() throws Exception {
        MvcResult r = mockMvc.perform(get("/index").session(sessionOf(newUser("admin")))).andReturn();
        assertEquals(200, r.getResponse().getStatus());
        assertTrue(body(r).contains("数据总览"));
    }

    // ================= CSRF：无 token 拒绝 =================

    @Test
    void POST缺少CSRF令牌返回403() throws Exception {
        MockHttpSession s = sessionOf(newUser("student"));
        MvcResult r = mockMvc.perform(post("/registration/join").session(s)
                .param("activityId", "1")).andReturn();
        assertEquals(403, r.getResponse().getStatus());
    }

    // ================= H2：取消归属校验 =================

    @Test
    void 取消他人报名被拒绝() throws Exception {
        User owner = newUser("student");
        User attacker = newUser("student");
        Activity a = newActivity("IT取消活动", 10);
        Registration reg = newRegistration(owner, a, "registered");

        MvcResult r = mockMvc.perform(post("/registration/cancel").session(sessionOf(attacker))
                .param("id", String.valueOf(reg.getId())).param("_csrf", CSRF)).andReturn();
        assertEquals("/registration/my", r.getResponse().getRedirectedUrl());
        assertEquals("无权操作他人的报名记录", r.getFlashMap().get("error"));
        assertEquals("registered", registrationService.getById(reg.getId()).getStatus());
    }

    @Test
    void 取消本人报名成功() throws Exception {
        User u = newUser("student");
        Activity a = newActivity("IT本人取消活动", 10);
        Registration reg = newRegistration(u, a, "registered");

        MvcResult r = mockMvc.perform(post("/registration/cancel").session(sessionOf(u))
                .param("id", String.valueOf(reg.getId())).param("_csrf", CSRF)).andReturn();
        assertEquals("已撤销报名申请", r.getFlashMap().get("success"));
        assertEquals("cancelled", registrationService.getById(reg.getId()).getStatus());
    }

    // ================= M1/M2：名额与重复报名（顺序不变量） =================

    @Test
    void 名额已满拦截第二名学生() throws Exception {
        Activity a = newActivity("IT名额活动", 1);
        User s1 = newUser("student"), s2 = newUser("student");

        mockMvc.perform(post("/registration/join").session(sessionOf(s1))
                .param("activityId", String.valueOf(a.getId())).param("_csrf", CSRF));
        MvcResult r = mockMvc.perform(post("/registration/join").session(sessionOf(s2))
                .param("activityId", String.valueOf(a.getId())).param("_csrf", CSRF)).andReturn();

        assertEquals("报名名额已满", r.getFlashMap().get("error"));
        assertEquals(1, registrationService.count(Wrappers.lambdaQuery(Registration.class)
                .eq(Registration::getActivityId, a.getId())), "名额1只应产生1条报名");
    }

    @Test
    void 同一学生重复报名被拦截() throws Exception {
        Activity a = newActivity("IT重复报名活动", 10);
        User s1 = newUser("student");

        mockMvc.perform(post("/registration/join").session(sessionOf(s1))
                .param("activityId", String.valueOf(a.getId())).param("_csrf", CSRF));
        MvcResult r = mockMvc.perform(post("/registration/join").session(sessionOf(s1))
                .param("activityId", String.valueOf(a.getId())).param("_csrf", CSRF)).andReturn();

        assertEquals("您已报名该活动，请勿重复提交", r.getFlashMap().get("error"));
        assertEquals(1, registrationService.count(Wrappers.lambdaQuery(Registration.class)
                .eq(Registration::getActivityId, a.getId())));
    }

    // ================= M2/L6/N3：审批全链路 =================

    @Test
    void 审批链_通过_拒绝带原因_重审清空() throws Exception {
        Activity a = newActivity("IT审批链活动", 5);
        User stu = newUser("student");
        Registration reg = newRegistration(stu, a, "registered");
        MockHttpSession adm = sessionOf(newUser("admin"));
        String rid = String.valueOf(reg.getId());

        // approve：状态 + 人数同步
        mockMvc.perform(post("/registration/approve").session(adm).param("id", rid).param("_csrf", CSRF));
        assertEquals("approved", registrationService.getById(reg.getId()).getStatus());
        assertEquals(1, activityService.getById(a.getId()).getCurrentParticipants(), "M2:人数应重算同步");

        // reject 带原因：状态 + remark + 人数回落
        mockMvc.perform(post("/registration/reject").session(adm).param("id", rid)
                .param("remark", "IT测试拒绝原因").param("_csrf", CSRF));
        Registration rj = registrationService.getById(reg.getId());
        assertEquals("rejected", rj.getStatus());
        assertEquals("IT测试拒绝原因", rj.getRemark(), "L6:拒绝原因应写入");
        assertEquals(0, activityService.getById(a.getId()).getCurrentParticipants());

        // reaudit：状态回待审核 + remark 清空
        mockMvc.perform(post("/registration/reaudit").session(adm).param("id", rid).param("_csrf", CSRF));
        Registration re = registrationService.getById(reg.getId());
        assertEquals("registered", re.getStatus());
        assertEquals("", re.getRemark(), "重审应清空上次审核备注");
    }

    // ================= M3/N1：发布校验 =================

    @Test
    void 发布活动_缺时间_超长标题_合法创建() throws Exception {
        MockHttpSession adm = sessionOf(newUser("admin"));

        // 缺时间（M3）
        MvcResult r1 = mockMvc.perform(post("/activity/save").session(adm)
                .param("title", "IT缺时间活动").param("_csrf", CSRF)).andReturn();
        assertEquals("/activity/toAdd", r1.getResponse().getRedirectedUrl());
        assertEquals("开始时间和结束时间不能为空", r1.getFlashMap().get("error"));

        // 超长标题（N1）
        MvcResult r2 = mockMvc.perform(post("/activity/save").session(adm)
                .param("title", "A".repeat(101))
                .param("startTime", "2027-06-01T09:00").param("endTime", "2027-06-02T18:00")
                .param("_csrf", CSRF)).andReturn();
        assertEquals("活动标题不能超过100个字符", r2.getFlashMap().get("error"));
        assertEquals(0, activityService.count(Wrappers.lambdaQuery(Activity.class)
                .likeRight(Activity::getTitle, "IT")), "违规数据不应落库");

        // 合法创建
        MvcResult r3 = mockMvc.perform(post("/activity/save").session(adm)
                .param("title", "IT合法活动").param("location", "IT楼")
                .param("startTime", "2027-06-01T09:00").param("endTime", "2027-06-02T18:00")
                .param("maxParticipants", "0").param("status", "published")
                .param("_csrf", CSRF)).andReturn();
        assertEquals("/activity/list", r3.getResponse().getRedirectedUrl());
        assertEquals(1, activityService.count(Wrappers.lambdaQuery(Activity.class)
                .eq(Activity::getTitle, "IT合法活动")));
    }

    // ================= L1/N2：分页 =================

    @Test
    void 分页渲染与越界重定向() throws Exception {
        MockHttpSession stu = sessionOf(newUser("student"));
        for (int i = 1; i <= 10; i++) {
            newActivity("IT分页活动" + i, 0);
        }
        long total = activityService.count();
        int pages = (int) ((total + 8) / 9);
        assertTrue(pages >= 2, "夹具应保证至少2页");

        MvcResult p1 = mockMvc.perform(get("/activity/list").session(stu)).andReturn();
        assertEquals(200, p1.getResponse().getStatus());
        List<?> first = (List<?>) p1.getModelAndView().getModel().get("activityList");
        assertEquals(9, first.size(), "第1页应为9条");

        MvcResult pOver = mockMvc.perform(get("/activity/list").session(stu)
                .param("page", String.valueOf(pages + 50))).andReturn();
        assertTrue(pOver.getResponse().getRedirectedUrl().contains("page=" + pages),
                "越界页码应重定向到末页(N2)，实际: " + pOver.getResponse().getRedirectedUrl());

        MvcResult pNeg = mockMvc.perform(get("/activity/list").session(stu).param("page", "-3")).andReturn();
        assertEquals(200, pNeg.getResponse().getStatus(), "负页码应clamp到第1页");
    }
}
