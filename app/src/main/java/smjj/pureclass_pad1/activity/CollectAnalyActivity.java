package smjj.pureclass_pad1.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import smjj.pureclass_pad1.R;
import smjj.pureclass_pad1.common.BaseActivity;
import smjj.pureclass_pad1.common.SPCommonInfoBean;
import smjj.pureclass_pad1.util.ActivityManage;


//汇总分析
public class CollectAnalyActivity extends BaseActivity {

    private Context context;
    private Handler mHandler;

    private SharedPreferences spConfig;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collect_analy);

        ActivityManage.addActivity(this);
        mHandler = new Handler();
        spConfig = getSharedPreferences(SPCommonInfoBean.SPName, MODE_PRIVATE);
        context = this;

    }
}
