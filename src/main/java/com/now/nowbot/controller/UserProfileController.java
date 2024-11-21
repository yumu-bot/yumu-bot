package com.now.nowbot.controller;

import com.now.nowbot.entity.UserProfileItem;
import com.now.nowbot.mapper.UserProfileItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@ResponseBody
@CrossOrigin({"http://localhost:5173", "https://siyuyuko.github.io", "https://a.yasunaori.be"})
@RequestMapping(value = "/pub/profile", method = RequestMethod.GET)
public class UserProfileController {
    private final UserProfileItemRepository userProfileItemRepository;

    @Autowired
    public UserProfileController(UserProfileItemRepository userProfileItemRepository) {
        this.userProfileItemRepository = userProfileItemRepository;
    }

    @GetMapping("list")
    public List<UserProfileItem> list() {
        var list = userProfileItemRepository.getAllUnaudited();
        list.forEach(e -> {
            var s = e.getPath();
            e.setPath(s);
        });

        return list;
    }
}
