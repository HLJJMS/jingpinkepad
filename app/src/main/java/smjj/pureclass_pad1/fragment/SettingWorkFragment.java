package smjj.pureclass_pad1.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;

import org.json.JSONException;
import org.ksoap2.serialization.SoapObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import smjj.pureclass_pad1.R;
import smjj.pureclass_pad1.activity.ClassAfterActivity;
import smjj.pureclass_pad1.activity.SelectExercisesActivity;
import smjj.pureclass_pad1.activity.WorkConditionActivity;
import smjj.pureclass_pad1.beans.SchedulingTableBean;
import smjj.pureclass_pad1.beans.StuSetWorkBean;
import smjj.pureclass_pad1.common.CommonAdapter;
import smjj.pureclass_pad1.common.CommonWay;
import smjj.pureclass_pad1.common.Constants;
import smjj.pureclass_pad1.common.SPCommonInfoBean;
import smjj.pureclass_pad1.common.ViewHolder;
import smjj.pureclass_pad1.network.WebServiceUtils;
import smjj.pureclass_pad1.util.AlertDialogUtil;
import smjj.pureclass_pad1.util.FormatUtils;
import smjj.pureclass_pad1.util.MD5;
import smjj.pureclass_pad1.util.PopupWindowUtil;
import smjj.pureclass_pad1.xorzlistview.xlistview.XListView;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by wlm on 2017/7/21.
 */
//布置作业碎片
public class SettingWorkFragment extends Fragment implements AdapterView.OnItemClickListener, XListView.IXListViewListener{
    private XListView listView;
    private TextView tv_school_name;

    private Context context;
    private Handler mHandler;

    private List<SchedulingTableBean.TablesBean.TableBean.RowsBean> listData;
    private CommonAdapter adapter;
    private SharedPreferences spConfig;
    private boolean isSettingWork;
    private String classId;
    private String typleUser;
    private String userCode;
    private ClassAfterActivity activity;

    private int pageNum = 1;
    //1代表未布置作业， 2代表一布置作业
    private String status;

    private List<StuSetWorkBean.RowsBean> studentList;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
        mHandler = new Handler();
        activity = (ClassAfterActivity) getActivity();
        spConfig = context.getSharedPreferences(SPCommonInfoBean.SPName, MODE_PRIVATE);
        typleUser = spConfig.getString(SPCommonInfoBean.typeUser,"");
        userCode = spConfig.getString(SPCommonInfoBean.userCode,"");
        classId = spConfig.getString(SPCommonInfoBean.classId,"");

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.fragment_setting_work, container, false);

        isSettingWork = true;

        findView(contentView);

        return contentView;
    }


    private void findView(View view) {
        listView = (XListView) view.findViewById(R.id.scheduling_lv);

        tv_school_name = (TextView) view.findViewById(R.id.tv_school_name);

        if (typleUser.equals("0")){
            tv_school_name.setText(R.string.school_name);
        }else {
            tv_school_name.setText(R.string.class_name);
        }

        listData = new ArrayList<>();
        studentList = new ArrayList<>();

        adapter = new CommonAdapter<SchedulingTableBean.TablesBean.TableBean.RowsBean>(context, listData, R.layout.item_schedu_lv1) {
            @Override
            public void convert(ViewHolder holder, final SchedulingTableBean.TablesBean.TableBean.RowsBean rowsBean) {
                int i = listData.indexOf(rowsBean);
                holder.setText(R.id.sd_no_tv, rowsBean.getClassId());

                holder.setText(R.id.sd_schgegin_tv, rowsBean.getClassname());
                holder.setText(R.id.sd_school_tv, rowsBean.getSchgegin());

                holder.setText(R.id.sd_class_type_tv, rowsBean.getClassType());
                holder.setText(R.id.sd_subjects_tv, rowsBean.getGname() + rowsBean.getSubjects());
                if (rowsBean.getTimeStar() != null && rowsBean.getTimeEnd() != null){
                    holder.setText(R.id.sd_class_date_tv, rowsBean.getTimeStar().substring(0,10));
                    holder.setText(R.id.sd_start_date_tv, rowsBean.getTimeStar().substring(11,16)
                            + "-" + rowsBean.getTimeEnd().substring(11,16));
                }
                String weekDay = FormatUtils.getWeekDay(FormatUtils.getCalendarFromString(rowsBean.getTimeStar().substring(0,10)));
                if (weekDay != null && !weekDay.equals("")){
                    holder.setText(R.id.week_day, weekDay);
                }
                holder.getView(R.id.iv_stu_name).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        classId = rowsBean.getClassId();
                        studentList.clear();
                        getSetWorkStu();
                    }
                });
            }
        };

        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
        listView.setXListViewListener(this);
        listView.setPullLoadEnable(false);//激活加载更多
        listView.setPullRefreshEnable(true);

        refreshData(isSettingWork);

    }


    @Override
    public void onRefresh() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //下拉刷新
                onLoad();
                listData.clear();
                pageNum = 1;
                requestScdData();
            }
        }, 500);

    }

    @Override
    public void onLoadMore() {

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //上拉加载更多
                onLoad();
                pageNum ++;
                requestScdData();

            }
        }, 500);
    }

    private void onLoad() {
        listView.stopRefresh();
        listView.stopLoadMore();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        listView.setRefreshTime(df.format(new Date()));
    }



    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        classId = listData.get(i - 1).getClassId();
        String subject = listData.get(i - 1).getSubjects();
        String grade = listData.get(i - 1).getGname();
        spConfig.edit().putString(SPCommonInfoBean.classId, classId).commit();
        if (isSettingWork){
            Intent intent = new Intent(context, SelectExercisesActivity.class);
            intent.putExtra("subject", subject);
            intent.putExtra("grade", grade);
            startActivity(intent);
        }else {
            Intent intent = new Intent(context, WorkConditionActivity.class);
//            Intent intent = new Intent(context, WorkConditionActivity1.class);
            intent.putExtra("enterMark", "1");
            startActivity(intent);
        }
    }

    public void refreshData(boolean isSettingWork1){
        isSettingWork = isSettingWork1;
        pageNum = 1;
        listData.clear();
        if (isSettingWork){
            status = "1";
        }else {
            status = "2";
        }
        requestScdData();
        adapter.setData(listData);
        adapter.notifyDataSetChanged();
    }


    private void showStudentPopu(){

        View view = LayoutInflater.from(context).inflate(R.layout.pop_student_view, null);

        final PopupWindow popupWindow = PopupWindowUtil.getPopuWindow(context,view,2,2,0.5f,-1);

        GridView gridView = (GridView) view.findViewById(R.id.gridview);
        TextView textView = (TextView) view.findViewById(R.id.tv_student_list);
        textView.setText("学生列表");
        CommonAdapter adapter = new CommonAdapter<StuSetWorkBean.RowsBean>(context, studentList, R.layout.item_pop_student) {
            @Override
            public void convert(ViewHolder holder, StuSetWorkBean.RowsBean rowsBean) {
                holder.setText(R.id.student_gridview_tv,rowsBean.getStudentName());
            }
        };
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                popupWindow.dismiss();
            }
        });

        ImageView imageView1 = (ImageView) view.findViewById(R.id.iv_close_pop);
        imageView1.setVisibility(View.VISIBLE);
        imageView1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupWindow.dismiss();
            }
        });
        popupWindow.showAtLocation(tv_school_name, Gravity.CENTER,0,0);

    }



    //请求排课列表(布置作业，查看作业的)
    public void requestScdData() {
        String UserCode = spConfig.getString(SPCommonInfoBean.userCode, "");

        if (CommonWay.netWorkCheck(context)) { // 在有网络的情况下登入
            if (UserCode == null || UserCode.equals("")) {
                Toast.makeText(context, "账号异常请重新登陆！", Toast.LENGTH_SHORT).show();
                return;
            }

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("UserCode", UserCode);
//            jsonObject.put("pageindex", pageNum + "");
            jsonObject.put("TestType", "3");
            jsonObject.put("Status", status);
            jsonObject.put("TypeUser",typleUser);


            final String jsonCode = jsonObject.toJSONString();
            String MD5Result = MD5.stringMd5(jsonCode + Constants.key);
            String methodName = Constants.GetSetWorkScdListMN;

            HashMap<String, String> map = new HashMap<>();
            map.put("jsoncode", jsonCode);
            map.put("token", MD5Result);
            activity.showLoading1();
            //通过工具类调用WebService接口
            WebServiceUtils.callWebService(Constants.WEB_SERVER_URL, methodName, map, new WebServiceUtils.WebServiceCallBack() {
                //WebService接口返回的数据回调到这个方法中
                @Override
                public void callBackSuccess(SoapObject result) {
                    activity.closeLoading();
                    //关闭进度条
                    if (result != null) {

                        org.json.JSONObject jsonObject = CommonWay.parseSoapObjectUnicode(result);
                        if (jsonObject != null) {
                            try {
                                String resultvalue = "";
                                if (jsonObject.toString().contains("resultvalue")) {
                                    resultvalue = jsonObject.getString("resultvalue");
                                }
                                if (resultvalue.equals("1") || resultvalue.equals("-1")) {
                                    String errinfo = jsonObject.getString("errinfo");
                                    //校验失败 是指token校验失败
                                    AlertDialogUtil.showAlertDialog(context, "提示", "没有未布置的排课列表");
                                } else {
                                    Gson gson = new Gson();
                                    SchedulingTableBean schedulingTableBean = gson.fromJson(jsonObject.toString(), SchedulingTableBean.class);
                                    if (schedulingTableBean != null) {
                                        if (schedulingTableBean.getTables().getTable().getRows().size() != 0){
                                            listData.addAll(schedulingTableBean.getTables().getTable().getRows());
//                                            FormatUtils.getListRank(listData);
                                            adapter.setData(listData);
                                            adapter.notifyDataSetChanged();
                                        }else {
                                          if (pageNum == 1){
                                              AlertDialogUtil.showAlertDialog(context, "提示", "该老师无相对应的排课记录");
                                          }else {
                                              AlertDialogUtil.showAlertDialog(context, "提示", "该老师无更多的排课记录");
                                          }
                                        }
                                    } else {
                                        AlertDialogUtil.showAlertDialog(context, "提示", "该老师无相对应的排课记录");
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                                AlertDialogUtil.showAlertDialog(context, "提示", "网络连接失败，请重试");
                            }
                        } else {
                            AlertDialogUtil.showAlertDialog(context, "提示", "服务器返回数据有误");

                        }

                    } else {
                        AlertDialogUtil.showAlertDialog(context, "提示", "网络连接失败，请重试");

                    }
                }
            });
        } else {
            AlertDialogUtil.showAlertDialog(context, "温馨提示", "哎呀,无网络连接,请检查您的网络设置！");
        }
    }


    //检查学生是否签到
    private void getSetWorkStu(){

        if (CommonWay.netWorkCheck(context)) { // 在有网络的情况下登入
            JSONObject jsonObject = new JSONObject();
            if (classId == null || classId.equals("")) {
                AlertDialogUtil.showAlertDialog(context,"提示","排课编号为空，请重新选择！");
                return;
            }

            jsonObject.put("LessonID", classId);
            jsonObject.put("UserCode", userCode);
            jsonObject.put("TestType", "3");
            jsonObject.put("Flag", status);

            final String jsonCode = jsonObject.toJSONString();
            String MD5Result = MD5.stringMd5(jsonCode + Constants.key);
            String methodName = Constants.GetStuSetWorkMN;
            HashMap<String, String> map = new HashMap<>();
            map.put("jsoncode", jsonCode);
            map.put("token", MD5Result);
            activity.showLoading();
            //通过工具类调用WebService接口
            WebServiceUtils.callWebService(Constants.WEB_SERVER_URL, methodName, map, new WebServiceUtils.WebServiceCallBack() {
                //WebService接口返回的数据回调到这个方法中
                @Override
                public void callBackSuccess(SoapObject result) {
                    activity.closeLoading();
                    //关闭进度条
                    if (result != null) {

                        org.json.JSONObject jsonObject = CommonWay.parseSoapObjectUnicode(result);
                        if (jsonObject != null) {
                            try {
                                String resultvalue = "";
                                if (jsonObject.toString().contains("resultvalue")) {
                                    resultvalue = jsonObject.getString("resultvalue");
                                }
                                if (resultvalue.equals("1")) {
                                    //未签到请求签到
                                    AlertDialogUtil.showAlertDialog(context, "提示", "该排课没有学生签到");
                                } else if (resultvalue.equals("-1")) {
                                    String errinfo = jsonObject.getString("errinfo");
                                    //校验失败 是指token校验失败
                                    Log.d("FAILURE_RESULT", errinfo);
                                    AlertDialogUtil.showAlertDialog(context, "提示", "效验失败");
                                }else {
                                    //已签到
                                    Gson gson = new Gson();
                                    StuSetWorkBean stuSetWorkBean = gson.fromJson(jsonObject.toString(), StuSetWorkBean.class);
                                    if (stuSetWorkBean != null) {
                                        studentList.addAll(stuSetWorkBean.getRows());
                                        if (studentList.size() == 0){
                                            AlertDialogUtil.showAlertDialog(context, "提示", "该排课没有学生签到");
                                            return;
                                        }
                                        showStudentPopu();
                                    } else {
                                        AlertDialogUtil.showAlertDialog(context, "提示", "该排课没有学生签到");
                                    }

                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                                AlertDialogUtil.showAlertDialog(context, "提示", "网络连接失败，请重试");
                            }
                        } else {
                            AlertDialogUtil.showAlertDialog(context, "提示", "服务器返回数据有误");
                        }
                    } else {
                        AlertDialogUtil.showAlertDialog(context, "提示", "网络连接失败，请重试");
                    }
                }
            });
        } else {
            AlertDialogUtil.showAlertDialog(context, "温馨提示", "哎呀,无网络连接,请检查您的网络设置！");
        }

    }



}
