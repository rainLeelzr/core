package avatar.rain.core.api;

import avatar.rain.result.Result;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Resource
    private LocalApiManager apiManager;

    @RequestMapping()
    public Result getAllApis() {
        Result result = new Result(1, "成功", apiManager.getApis());
        return result;
    }

    @RequestMapping("/initTime")
    public long getInitTime() {
        return apiManager.getInitTime();
    }
}
