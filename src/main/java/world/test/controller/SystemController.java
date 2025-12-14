package world.test.controller;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Controller;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import world.test.constant.MedicalConstants;
import world.test.entity.*;
import world.test.utils.Assert;

import java.math.BigDecimal;
import java.util.*;

/**
 * 系统跳转控制器
 */
@Controller
public class SystemController extends BaseController<User> {

    /**
     * 首页
     */
    @GetMapping("/")
    public String index(Map<String, Object> map) {
        return "login";
    }

    /**
     * 注册
     */
    @GetMapping("/register")
    public String register(Map<String, Object> map) {
        return "register";
    }

    /**
     * 登录
     */
    @GetMapping("/login")
    public String login(Map<String, Object> map) {
        return "login";
    }

    /**
     * 忘记密码
     */
    @GetMapping("/forgot")
    public String forgot(Map<String, Object> map) {
        return "forgot";
    }

    /**
     * 退出登录
     */
    @GetMapping("/logout")
    public String logout() {
        session.invalidate();
        return "redirect:/index.html";
    }

    /**
     * 所有反馈
     */
    @GetMapping("/all-feedback")
    public String feedback(Map<String, Object> map) {
        if (Assert.isEmpty(loginUser)) {
            return "redirect:/index.html";
        }
        List<Feedback> feedbackList = feedbackService.all();

        map.put("feedbackList", feedbackList);
        return "all-feedback";
    }

    /**
     * 我的资料
     */
    @GetMapping("/profile")
    public String profile(Map<String, Object> map) {
        if (Assert.isEmpty(loginUser)) {
            return "redirect:/index.html";
        }
        return "profile";
    }


    /**
     * 查询相关疾病下的药
     */
    @GetMapping("globalSelect")
    public String globalSelect(Map<String, Object> map, String nameValue) {
        nameValue = nameValue.replace("，", ",");
        List<String> idArr = Arrays.asList(nameValue.split(","));
        //首先根据关键字去查询
        Set<Illness> illnessSet = new HashSet<>();
        idArr.forEach(s -> {
            Illness one = illnessService.getOne(new QueryWrapper<Illness>().like("illness_name", s));
            if (ObjectUtil.isNotNull(one)) {
                illnessSet.add(one);
            }
        });
        idArr.forEach(s -> {
            Illness one = illnessService.getOne(new QueryWrapper<Illness>().like("special_symptom", s));
            if (ObjectUtil.isNotNull(one)) {
                illnessSet.add(one);
            }
        });
        idArr.forEach(s -> {
            Illness one = illnessService.getOne(new QueryWrapper<Illness>().like("illness_symptom", s));
            if (ObjectUtil.isNotNull(one)) {
                illnessSet.add(one);
            }
        });
        map.put("illnessSet", illnessSet);
        return "index";
    }

    /**
     * 添加疾病页面
     */
    @GetMapping("add-illness")
    public String addIllness(Integer id, Map<String, Object> map) {
        if (Assert.isEmpty(loginUser)) {
            return "redirect:/index.html";
        }
        Illness illness = new Illness();
        if (Assert.notEmpty(id)) {
            illness = illnessService.get(id);
        }
        List<IllnessKind> illnessKinds = illnessKindService.all();
        map.put("illness", illness);
        map.put("kinds", illnessKinds);
        return "add-illness";
    }

    /**
     * 添加药品页面
     */
    @GetMapping("add-medical")
    public String addMedical(Integer id, Map<String, Object> map) {
        if (Assert.isEmpty(loginUser)) {
            return "redirect:/index.html";
        }
        List<Illness> illnesses = illnessService.all();
        Medicine medicine = new Medicine();
        if (Assert.notEmpty(id)) {
            medicine = medicineService.get(id);
            for (Illness illness : illnesses) {
                List<IllnessMedicine> query = illnessMedicineService.query(IllnessMedicine.builder().medicineId(id).illnessId(illness.getId()).build());
                if (Assert.notEmpty(query)) {
                    illness.setIllnessMedicine(query.get(0));
                }
            }
        }
        map.put("illnesses", illnesses);
        map.put("medicine", medicine);
        return "add-medical";
    }

    /**
     * 疾病管理页面
     */
    @GetMapping("all-illness")
    public String allIllness(Map<String, Object> map) {
        if (Assert.isEmpty(loginUser)) {
            return "redirect:/index.html";
        }
        List<Illness> illnesses = illnessService.all();
        for (Illness illness : illnesses) {
            illness.setKind(illnessKindService.get(illness.getKindId()));
        }
        map.put("illnesses", illnesses);
        return "all-illness";
    }

    /**
     * 药品管理页面
     */
    @GetMapping("all-medical")
    public String allMedical(Map<String, Object> map) {
        if (Assert.isEmpty(loginUser)) {
            return "redirect:/index.html";
        }
        List<Medicine> medicines = medicineService.all();
        map.put("medicines", medicines);
        return "all-medical";
    }
    /**
     * 购物车
     */
    @GetMapping("findcart")
    public String cart(Map<String, Object> map, String nameValue, Integer page) {
        if (Assert.isEmpty(loginUser)) {
            return "redirect:/index.html";
        }
        if (nameValue == null) {
//            nameValue = loginUser.getUserName(); //获得真实姓名
            nameValue = loginUser.getUserAccount(); //获得用户名
        }

        page = ObjectUtils.isEmpty(page) ? 1 : page;
        //无敌了 表里面存的叫userName 但实际上对应userAccount

        Map<String, Object> goodsMap = goodsService.getGoodsList(nameValue, page);
        List<Goods> goodsList = (List<Goods>) goodsMap.get("goodsList");
        List<Object> goodsAndMedicine = new ArrayList<>();
        for (Goods goods : goodsList) {
            //根据goods里面的medicineName去查询medicine
            Medicine medicine = medicineService.getMedicineListByName(goods.getMedicineName());
            goodsAndMedicine.add(new Object[]{goods, medicine});
        }

//       计算总价  先获取所有 goodsService.getGoodsListTotal(nameValue);
        Map<String, Object> goods = goodsService.getGoodsListTotal(nameValue);
        map.put("goodsAndMedicineList", goodsAndMedicine);

        BigDecimal total = BigDecimal.valueOf(0);
        for (Goods goods1 : goodsList) {
            total = total.add(goods1.getMedicinePrice().multiply(BigDecimal.valueOf(goods1.getMedicineNum())));
        }
        map.put("total", total);

        return "cart";
    }

}


