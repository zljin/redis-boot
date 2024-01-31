package com.zou.controller;

import com.zou.pojo.dto.LoginFormDTO;
import com.zou.pojo.dto.UserDTO;
import com.zou.pojo.entity.Shop;
import com.zou.service.AppService;
import com.zou.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/app")
public class AppController {

    private final AppService appService;

    @Autowired
    public AppController(AppService appService) {
        this.appService = appService;
    }

    // 发送短信验证码并保存验证码
    @PostMapping("code")
    public ResponseEntity<?> sendCode(@RequestParam("phone") String phone) {
        return appService.sendCode(phone);
    }

    @PostMapping(value = "/login")
    public ResponseEntity<String> login(@RequestBody LoginFormDTO loginForm) throws Exception {
        return appService.login(loginForm);
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> me() {
        UserDTO user = UserHolder.getUser();
        return ResponseEntity.ok(user);
    }

    @GetMapping("/shop/{id}")
    public ResponseEntity<Shop> queryShopById(@PathVariable("id") Long id) {
        return appService.queryShopById(id);
    }

    @GetMapping("/shop/list")
    public ResponseEntity<List<Shop>> queryShopList() {
        return appService.queryShopList();
    }

    /**
     * 根据商铺类型分页查询商铺信息
     *
     * @param typeId 商铺类型
     * @return 商铺列表
     */
    @GetMapping("/shop/of/type")
    public ResponseEntity<List<Shop>> queryShopByType(
            @RequestParam("typeId") Integer typeId,
            @RequestParam(value = "x", required = false) Double x,
            @RequestParam(value = "y", required = false) Double y
    ) {
        return appService.queryShopByType(typeId, x, y);
    }

    @PutMapping("/blog/like/{id}")
    public ResponseEntity<?> likeBlog(@PathVariable("id") Long id) {
        return appService.likeBlog(id);
    }

    @GetMapping("/blog/likes/{id}")
    public ResponseEntity<List<String>> queryBlogLikes(@PathVariable("id") Long id) {
        return appService.queryBlogLikes(id);
    }

}
