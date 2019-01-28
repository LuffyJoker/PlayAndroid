import android.app.Application;

import com.blankj.utilcode.util.Utils;

/**
 * Created by Mr.Q on 2019/1/28.
 * 描述：
 */

public class BaseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // 工具代码库初始化
        Utils.init(this);
    }
}
