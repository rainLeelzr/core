package avatar.rain.core.api;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Resource
    private LocalApiManager apiManager;

    @RequestMapping()
    public ServerApi getAllApis() {
        return apiManager.getServerApi();
    }

    @RequestMapping("/initTime")
    public long getInitTime() {
        return apiManager.getInitTime();
    }
}
