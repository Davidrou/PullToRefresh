package com.example.pulltorefreshtest;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.View.OnTouchListener;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;


public class MyRefreshableView extends LinearLayout implements OnTouchListener {

    /**
     * 用于存储上次更新时间
     */
    private SharedPreferences preferences;

    /**
     * 下拉头的View
     */
    private View header;

    /**
     * 需要去下拉刷新的ListView
     */
    private ListView listView;

    /**
     * 刷新时显示的进度条
     */
    private ProgressBar progressBar;

    /**
     * 指示下拉和释放的箭头
     */
    private ImageView arrow;

    /**
     * 指示下拉和释放的文字描述
     */
    private TextView description;

    /**
     * 上次更新时间的文字描述
     */
    private TextView updateAt;
    /**
     * 在被判定为滚动之前用户手指可以移动的最大值。
     */
    private int touchSlop;
    /**
     * 下拉状态
     */
    public static final int STATUS_PULL_TO_REFRESH = 0;

    /**
     * 释放立即刷新状态
     */
    public static final int STATUS_RELEASE_TO_REFRESH = 1;

    /**
     * 正在刷新状态
     */
    public static final int STATUS_REFRESHING = 2;

    /**
     * 刷新完成或未刷新状态
     */
    public static final int STATUS_REFRESH_FINISHED = 3;

    /**
     * 当前是否可以下拉，只有ListView滚动到头的时候才允许下拉
     */
    private boolean ableToPull;
    /**
     * 手指按下时的屏幕纵坐标
     */
    private float yDown;
    /**
     * 下拉头的高度
     */
    private int hideHeaderHeight;
    /**
     * 下拉头的布局参数
     */
    private MarginLayoutParams headerLayoutParams;
    /**
     * 是否已加载过一次layout，这里onLayout中的初始化只需加载一次
     */
    private boolean loadOnce;

    /**
     * 下拉状态
     */
    private int pullStatus = STATUS_REFRESH_FINISHED;
    /**
     * 上一次的状态
     */
    private int lastStatus = STATUS_REFRESH_FINISHED;
    /**
     * 下拉头部回滚的速度
     */
    public static final int SCROLL_SPEED = -20;
    /**
     * 下拉刷新事件监听器
     */
    public PullToRefreshListener mListener;
    /**
     * 上次更新时间的字符串常量，用于作为SharedPreferences的键值
     */
    private static final String UPDATED_AT = "updated_at";
    /**
     * 上次更新时间的毫秒值
     */
    private long lastUpdateTime;
    /**
     * 一分钟的毫秒值，用于计算更新的时间
     */
    private static final long  ONE_MINUTE=60*1000;
    /**
     * 一小时的毫秒值，用于计算更新的时间
     */
    private static final long  ONE_HOUR=ONE_MINUTE*60;
    /**
     * 一天的毫秒值，用于判断上次的更新时间
     */
    public static final long ONE_DAY = 24 * ONE_HOUR;

    /**
     * 一月的毫秒值，用于判断上次的更新时间
     */
    public static final long ONE_MONTH = 30 * ONE_DAY;

    /**
     * 一年的毫秒值，用于判断上次的更新时间
     */
    public static final long ONE_YEAR = 12 * ONE_MONTH;

    /**
     * 下载刷新的构造函数
     *
     * @param context
     * @param attrs
     */
    public MyRefreshableView(Context context, AttributeSet attrs) {
        super(context, attrs);
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        header = LayoutInflater.from(context).inflate(R.layout.pull_to_refresh, null, true);
        progressBar = (ProgressBar) header.findViewById(R.id.progress_bar);
        arrow = (ImageView) header.findViewById(R.id.arrow);
        description = (TextView) header.findViewById(R.id.description);
        updateAt = (TextView) header.findViewById(R.id.updated_at);
        //触发移动事件的最小距离
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        refreshUpdatedAtValue();
        setOrientation(VERTICAL);
        addView(header, 0);
    }

    /**
     * 重写onLayout方法，在第一次调用时隐藏Header
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // TODO Auto-generated method stub
        System.out.println("onLayout");
        super.onLayout(changed, l, t, r, b);
        if (changed && !loadOnce) {
            hideHeaderHeight = -header.getHeight();
            headerLayoutParams = (MarginLayoutParams) header.getLayoutParams();
            headerLayoutParams.topMargin = hideHeaderHeight;
            listView = (ListView) getChildAt(1);
            listView.setOnTouchListener(this);
            loadOnce = true;
        }
    }


    private void refreshUpdatedAtValue()  {

        lastUpdateTime = preferences.getLong(UPDATED_AT , -1);
        long currentTime = System.currentTimeMillis();
        long timePassed = currentTime - lastUpdateTime;
        long timeIntoFormat;
        String updateAtValue;
        if (lastUpdateTime == -1) {
            updateAtValue = getResources().getString(R.string.not_updated_yet);
        } else if (timePassed < 0) {
            updateAtValue = getResources().getString(R.string.time_error);
        } else if (timePassed < ONE_MINUTE) {
            updateAtValue = getResources().getString(R.string.updated_just_now);
        } else if (timePassed < ONE_HOUR) {
            timeIntoFormat = timePassed / ONE_MINUTE;
            String value = timeIntoFormat + "分钟";
            updateAtValue = String.format(getResources().getString(R.string.updated_at), value);
        } else if (timePassed < ONE_DAY) {
            timeIntoFormat = timePassed / ONE_HOUR;
            String value = timeIntoFormat + "小时";
            updateAtValue = String.format(getResources().getString(R.string.updated_at), value);
        } else if (timePassed < ONE_MONTH) {
            timeIntoFormat = timePassed / ONE_DAY;
            String value = timeIntoFormat + "天";
            updateAtValue = String.format(getResources().getString(R.string.updated_at), value);
        } else if (timePassed < ONE_YEAR) {
            timeIntoFormat = timePassed / ONE_MONTH;
            String value = timeIntoFormat + "个月";
            updateAtValue = String.format(getResources().getString(R.string.updated_at), value);
        } else {
            timeIntoFormat = timePassed / ONE_YEAR;
            String value = timeIntoFormat + "年";
            updateAtValue = String.format(getResources().getString(R.string.updated_at), value);
        }
        updateAt.setText(updateAtValue);


    }

    /**
     * 当ListView被触摸时调用，其中处理了各种下拉刷新的具体逻辑。
     */
    @Override
    public boolean onTouch(View arg0, MotionEvent event) {
        checkIsAbleToPull(event);
        if (ableToPull) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    yDown = event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    float yMove = event.getRawY();
                    int distance = (int) (yMove - yDown);
                    System.out.println(distance);
                    if (distance <= 0 && headerLayoutParams.topMargin <= hideHeaderHeight) {
                        return false;
                    }
                    if (distance < touchSlop) {
                        return false;
                    }
                    if (pullStatus!=STATUS_REFRESHING) {
                        // 通过偏移下拉头的topMargin值，来实现下拉效果
                        int newMargin = (distance / 2) + hideHeaderHeight;
                        headerLayoutParams.topMargin = newMargin;
                        header.setLayoutParams(headerLayoutParams);
                        //根据下拉的距离判断状态
                        if (headerLayoutParams.topMargin > 20) {
                            pullStatus = STATUS_RELEASE_TO_REFRESH;
                        }else{
                            pullStatus = STATUS_PULL_TO_REFRESH;
                        }
                    }else{
                        return  true;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                default:
                    if (pullStatus == STATUS_RELEASE_TO_REFRESH) {
                        // 松手时如果是释放立即刷新状态，就去调用正在刷新的任务
                       new RefreshingTask().execute();
                    } else if (pullStatus == STATUS_PULL_TO_REFRESH) {
                        // 松手时如果是下拉状态，就去调用隐藏下拉头的任务
                        new HideHeaderTask().execute();
                    }
                    break;
            }
            //Header已经显示出了，手还在屏幕上，正在拉或者向上滑
            if(pullStatus==STATUS_PULL_TO_REFRESH||pullStatus==STATUS_RELEASE_TO_REFRESH){
                listView.setPressed(false);
                updateHeaderview();
                return true;
            }
        }
        return false;
    }

    /**
     * 更新头部信息
     */
    private void updateHeaderview() {
        // TODO Auto-generated method stub
        if (lastStatus != pullStatus) {
            switch (pullStatus) {
                case STATUS_PULL_TO_REFRESH:
                    description.setText("下拉刷新");
                    lastStatus = STATUS_PULL_TO_REFRESH;
                    progressBar.setVisibility(View.INVISIBLE);
                    updateAt.setVisibility(View.VISIBLE);
                    rorateArrow();
                    break;
                case STATUS_RELEASE_TO_REFRESH:
                    description.setText("释放立即刷新");
                    progressBar.setVisibility(View.INVISIBLE);
                    updateAt.setVisibility(View.VISIBLE);
                    lastStatus = STATUS_RELEASE_TO_REFRESH;
                    rorateArrow();
                    break;
                case STATUS_REFRESHING:
                     description.setText("刷新中");
                     lastStatus=STATUS_REFRESHING;
                     arrow.clearAnimation();
                     arrow.setVisibility(View.GONE);
                     progressBar.setVisibility(View.VISIBLE);
                     updateAt.setVisibility(View.INVISIBLE);
            }
            refreshUpdatedAtValue();
        }
    }

    /**
     * 旋转箭头
     */
    private void rorateArrow() {
        // TODO Auto-generated method stub
        float pivotX = arrow.getWidth() / 2f;
        float pivotY = arrow.getHeight() / 2f;
        float fromDegrees = 0f;
        float toDegrees = 0f;
        if (pullStatus == STATUS_PULL_TO_REFRESH) {
            fromDegrees = 180f;
            toDegrees = 360f;
        } else if (pullStatus == STATUS_RELEASE_TO_REFRESH) {
            fromDegrees = 0f;
            toDegrees = 180f;
        }
        RotateAnimation animation = new RotateAnimation(fromDegrees, toDegrees, pivotX, pivotY);
        animation.setDuration(100);
        animation.setFillAfter(true);
        arrow.startAnimation(animation);
    }

    private void checkIsAbleToPull(MotionEvent event) {
        View firstChild = listView.getChildAt(0);
        if (firstChild != null) {
            int firstVisiblePos = listView.getFirstVisiblePosition();
            if (firstVisiblePos == 0 && firstChild.getTop() == 0) {
                if (!ableToPull) {
                    yDown = event.getRawY();
                }
                // 如果首个元素的上边缘，距离父布局值为0，就说明ListView滚动到了最顶部，此时应该允许下拉刷新
                ableToPull = true;
            } else {
                ableToPull = false;
            }
        } else {
            // 如果ListView中没有元素，也应该允许下拉刷新
            ableToPull = true;
        }
    }

    /**
     * 隐藏下拉头的任务，当未进行下拉刷新或下拉刷新完成后，此任务将会使下拉头重新隐藏。
     *
     */
    class HideHeaderTask extends  AsyncTask<Void,Integer,Void>{
        @Override
        protected Void doInBackground(Void... voids) {
                int topMagin=headerLayoutParams.topMargin;
                while(true){
                    topMagin+=SCROLL_SPEED;
                    if(topMagin<=hideHeaderHeight){
                        topMagin=hideHeaderHeight;
                        break;
                    }
                    publishProgress(topMagin);
                    sleep(10);
                }
                pullStatus=STATUS_REFRESH_FINISHED;
                publishProgress(0);
                return null;
            }

        @Override
        protected void onProgressUpdate(Integer... values) {
            updateHeaderview();
            headerLayoutParams.topMargin=values[0];
            header.setLayoutParams(headerLayoutParams);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            headerLayoutParams.topMargin=hideHeaderHeight;
            header.setLayoutParams(headerLayoutParams);
            pullStatus=STATUS_REFRESH_FINISHED;
        }
    }
    /**
     * 完成了下拉刷新任务
     */
    private void finishRefresh(){
        pullStatus=STATUS_REFRESH_FINISHED;
        new HideHeaderTask().execute();
        preferences.edit().putLong(UPDATED_AT,System.currentTimeMillis()).commit();
    }
    /**
     * 正在刷新的任务
     */
    class RefreshingTask extends AsyncTask<Void,Integer,Void>{

        @Override
        protected Void doInBackground(Void... voids) {
            int topMagin=headerLayoutParams.topMargin;
            while(true){
                topMagin+=SCROLL_SPEED;
                if(topMagin<0){
                    topMagin=0;
                    break;
                }
                publishProgress(topMagin);
                sleep(10);
            }
            pullStatus=STATUS_REFRESHING;
            publishProgress(0);
            //执行刷新任务
            if(mListener!=null) {
                mListener.onRefresh();
                finishRefresh();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            updateHeaderview();
            headerLayoutParams.topMargin=values[0];
            header.setLayoutParams(headerLayoutParams);
        }
    }

    /**
     * 休眠
     * @param i
     */
    private void sleep(long i) {
        try {
            Thread.sleep(i);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 下拉刷新的监听器，当触发刷新事件之后执行onRefresh()
     */
    public interface  PullToRefreshListener{
        void onRefresh();
    }

    /**
     * 给下拉刷新控件绑定一个刷新监听器
     * @param listener
     */
   public void setOnRefreshListener(PullToRefreshListener listener){
        mListener=listener;
   }
}
