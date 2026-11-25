package com.stu.huodongbm;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stu.huodongbm.entity.Activity;
import com.stu.huodongbm.entity.Registration;
import com.stu.huodongbm.entity.User;
import com.stu.huodongbm.service.ActivityService;
import com.stu.huodongbm.service.RegistrationService;
import com.stu.huodongbm.service.UserService;
import com.stu.huodongbm.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 最小集成测试套件（前后端分离 /api + JWT 版本）：覆盖登录与惰性哈希、暴力破解锁定、
 * 鉴权拦截、取消归属校验、名额与重复报名、审批全链路、发布校验、分页钳制等关键回归点。
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
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private UserService userService;
    @Autowired private ActivityService activityService;
    @Autowired private RegistrationService registrationService;

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

    /** 为指定用户签发 JWT（与 JwtInterceptor 的解析口径一致） */
    private String bearer(User u) {
        return "Bearer " + jwtUtil.generate(u.getId(), u.getUsername(), u.getRealName(), u.getRole());
    }

    private String body(MvcResult r) throws Exception {
        return r.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private JsonNode json(MvcResult r) throws Exception {
        return objectMapper.readTree(body(r));
    }

    private String loginJson(String username, String password) throws Exception {
        return objectMapper.writeValueAsString(Map.of("username", username, "password", password));
    }

    // ================= 登录、惰性哈希 =================

    @Test
    void 登录成功_返回token与用户_明文密码惰性升级() throws Exception {
        User u = newUser("student");

        MvcResult r = mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(u.getUsername(), "123456"))).andReturn();

        assertEquals(200, r.getResponse().getStatus());
        JsonNode data = json(r).get("data");
        assertTrue(data.get("token").asText().length() > 20, "应返回 JWT");
        assertEquals("student", data.get("user").get("role").asText());

        User after = userService.getById(u.getId());
        assertEquals(60, after.getPassword().length(), "BCrypt 哈希应为60位");
        assertTrue(after.getPassword().startsWith("$2"), "明文密码应惰性升级为哈希");
    }

    @Test
    void 登录密码错误_返回友好提示() throws Exception {
        User u = newUser("student");
        MvcResult r = mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(u.getUsername(), "wrong-pass"))).andReturn();

        assertEquals(500, json(r).get("code").asInt(), "业务失败以 body code 表达");
        assertTrue(body(r).contains("用户名或密码错误"));
    }

    // ================= 暴力破解锁定 =================

    @Test
    void 连续5次失败后第6次被锁定() throws Exception {
        String fake = "it_probe_" + UUID.randomUUID().toString().substring(0, 8);
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson(fake, "x")));
        }
        MvcResult r = mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(fake, "x"))).andReturn();
        assertTrue(body(r).contains("失败次数过多"), "第6次应触发锁定提示");
    }

    // ================= 鉴权拦截 =================

    @Test
    void 无token访问受保护接口返回401() throws Exception {
        MvcResult r = mockMvc.perform(get("/api/home")).andReturn();
        assertEquals(401, r.getResponse().getStatus());
    }

    @Test
    void 学生访问管理接口返回403() throws Exception {
        MvcResult r = mockMvc.perform(get("/api/dashboard")
                .header("Authorization", bearer(newUser("student")))).andReturn();
        assertEquals(403, r.getResponse().getStatus());
    }

    @Test
    void 管理员访问数据总览正常() throws Exception {
        MvcResult r = mockMvc.perform(get("/api/dashboard")
                .header("Authorization", bearer(newUser("admin")))).andReturn();
        assertEquals(200, r.getResponse().getStatus());
        JsonNode data = json(r).get("data");
        assertTrue(data.has("activityCount") && data.has("registrationCount"));
    }

    // ================= 取消归属校验 =================

    @Test
    void 取消他人报名被拒绝() throws Exception {
        User owner = newUser("student");
        User attacker = newUser("student");
        Activity a = newActivity("IT取消活动", 10);
        Registration reg = newRegistration(owner, a, "registered");

        MvcResult r = mockMvc.perform(post("/api/registrations/" + reg.getId() + "/cancel")
                .header("Authorization", bearer(attacker))).andReturn();

        assertEquals(403, json(r).get("code").asInt());
        assertTrue(body(r).contains("无权操作他人的报名记录"));
        assertEquals("registered", registrationService.getById(reg.getId()).getStatus());
    }

    @Test
    void 取消本人报名成功() throws Exception {
        User u = newUser("student");
        Activity a = newActivity("IT本人取消活动", 10);
        Registration reg = newRegistration(u, a, "registered");

        MvcResult r = mockMvc.perform(post("/api/registrations/" + reg.getId() + "/cancel")
                .header("Authorization", bearer(u))).andReturn();

        assertEquals(200, json(r).get("code").asInt());
        assertEquals("cancelled", registrationService.getById(reg.getId()).getStatus());
    }

    // ================= 名额与重复报名 =================

    @Test
    void 名额已满拦截第二名学生() throws Exception {
        Activity a = newActivity("IT名额活动", 1);
        User s1 = newUser("student"), s2 = newUser("student");

        mockMvc.perform(post("/api/registrations/join").param("activityId", String.valueOf(a.getId()))
                .header("Authorization", bearer(s1)));
        MvcResult r = mockMvc.perform(post("/api/registrations/join").param("activityId", String.valueOf(a.getId()))
                .header("Authorization", bearer(s2))).andReturn();

        assertEquals(400, json(r).get("code").asInt());
        assertTrue(body(r).contains("报名名额已满"));
        assertEquals(1, registrationService.count(Wrappers.lambdaQuery(Registration.class)
                .eq(Registration::getActivityId, a.getId())), "名额1只应产生1条报名");
    }

    @Test
    void 同一学生重复报名被拦截() throws Exception {
        Activity a = newActivity("IT重复报名活动", 10);
        User s1 = newUser("student");

        mockMvc.perform(post("/api/registrations/join").param("activityId", String.valueOf(a.getId()))
                .header("Authorization", bearer(s1)));
        MvcResult r = mockMvc.perform(post("/api/registrations/join").param("activityId", String.valueOf(a.getId()))
                .header("Authorization", bearer(s1))).andReturn();

        assertEquals(400, json(r).get("code").asInt());
        assertTrue(body(r).contains("您已报名该活动，请勿重复提交"));
        assertEquals(1, registrationService.count(Wrappers.lambdaQuery(Registration.class)
                .eq(Registration::getActivityId, a.getId())));
    }

    // ================= 审批全链路 =================

    @Test
    void 审批链_通过_拒绝带原因_重审清空() throws Exception {
        Activity a = newActivity("IT审批链活动", 5);
        User stu = newUser("student");
        Registration reg = newRegistration(stu, a, "registered");
        String adm = bearer(newUser("admin"));
        String base = "/api/registrations/" + reg.getId();

        // approve：状态 + 人数同步
        mockMvc.perform(post(base + "/approve").header("Authorization", adm));
        assertEquals("approved", registrationService.getById(reg.getId()).getStatus());
        assertEquals(1, activityService.getById(a.getId()).getCurrentParticipants(), "人数应重算同步");

        // reject 带原因：状态 + remark + 人数回落
        mockMvc.perform(post(base + "/reject").param("remark", "IT测试拒绝原因")
                .header("Authorization", adm));
        Registration rj = registrationService.getById(reg.getId());
        assertEquals("rejected", rj.getStatus());
        assertEquals("IT测试拒绝原因", rj.getRemark(), "拒绝原因应写入");
        assertEquals(0, activityService.getById(a.getId()).getCurrentParticipants());

        // reaudit：状态回待审核 + remark 清空
        mockMvc.perform(post(base + "/reaudit").header("Authorization", adm));
        Registration re = registrationService.getById(reg.getId());
        assertEquals("registered", re.getStatus());
        assertEquals("", re.getRemark(), "重审应清空上次审核备注");
    }

    // ================= 发布校验 =================

    @Test
    void 发布活动_缺时间_超长标题_时间倒置_合法创建() throws Exception {
        String adm = bearer(newUser("admin"));

        // 缺时间
        MvcResult r1 = mockMvc.perform(multipart("/api/activities")
                .param("title", "IT缺时间活动")
                .header("Authorization", adm)).andReturn();
        assertEquals(400, json(r1).get("code").asInt());
        assertTrue(body(r1).contains("开始时间和结束时间不能为空"));

        // 超长标题
        MvcResult r2 = mockMvc.perform(multipart("/api/activities")
                .param("title", "A".repeat(101))
                .param("startTime", "2027-06-01T09:00").param("endTime", "2027-06-02T18:00")
                .header("Authorization", adm)).andReturn();
        assertEquals(400, json(r2).get("code").asInt());
        assertTrue(body(r2).contains("活动标题不能超过100个字符"));

        // 结束时间早于开始时间
        MvcResult r3 = mockMvc.perform(multipart("/api/activities")
                .param("title", "IT时间倒置活动")
                .param("startTime", "2027-06-02T09:00").param("endTime", "2027-06-01T18:00")
                .header("Authorization", adm)).andReturn();
        assertEquals(400, json(r3).get("code").asInt());
        assertTrue(body(r3).contains("结束时间必须晚于开始时间"));

        // 违规数据不应落库
        assertEquals(0, activityService.count(Wrappers.lambdaQuery(Activity.class)
                .likeRight(Activity::getTitle, "IT")), "违规数据不应落库");

        // 合法创建
        MvcResult r4 = mockMvc.perform(multipart("/api/activities")
                .param("title", "IT合法活动").param("location", "IT楼")
                .param("startTime", "2027-06-01T09:00").param("endTime", "2027-06-02T18:00")
                .param("maxParticipants", "0").param("status", "published")
                .header("Authorization", adm)).andReturn();
        assertEquals(200, json(r4).get("code").asInt());
        assertEquals(1, activityService.count(Wrappers.lambdaQuery(Activity.class)
                .eq(Activity::getTitle, "IT合法活动")));
    }

    // ================= 分页钳制 =================

    @Test
    void 分页越界钳制到末页_负页码钳制到首页() throws Exception {
        String stu = bearer(newUser("student"));
        for (int i = 1; i <= 10; i++) {
            newActivity("IT分页活动" + i, 0);
        }

        MvcResult pOver = mockMvc.perform(get("/api/activities").param("page", "999")
                .header("Authorization", stu)).andReturn();
        JsonNode over = json(pOver).get("data");
        assertEquals(over.get("totalPages").asInt(), over.get("currentPage").asInt(), "越界页码应钳制到末页");

        MvcResult pNeg = mockMvc.perform(get("/api/activities").param("page", "-3")
                .header("Authorization", stu)).andReturn();
        assertEquals(1, json(pNeg).get("data").get("currentPage").asInt(), "负页码应钳制到第1页");
    }
}
