package com.example.lenovo.myaexg.wifi;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.example.lenovo.myaexg.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.Random;

public class MpAndroidChart extends AppCompatActivity implements View.OnClickListener {
    private LineChart mChart;
    private Button mBtnClick;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mp_android_chart);

        initView();
        initData();
    }

    private void initView() {
        mChart = (LineChart) findViewById(R.id.lin);
        mBtnClick = (Button) findViewById(R.id.btn);
        mBtnClick.setOnClickListener(this);

    }

    private void initData() {
        //设置可以触摸
        mChart.setTouchEnabled(true);
        //设置可拖拽
        mChart.setDragEnabled(true);
        //设置可缩放
        mChart.setScaleEnabled(true);
        //设置图表网格背景
        mChart.setDrawGridBackground(false);
        //设置多点触控
        mChart.setPinchZoom(true);
        //设置图表的背景颜色
        mChart.setBackgroundColor(Color.WHITE);
        //设置折线图的数据
        LineData data = new LineData();
        //数据显示的颜色
        data.setValueTextColor(Color.BLACK);
        //先添加一个空的数据，随后往里面动态添加
        mChart.setData(data);
        // 图表的注解(只有当数据集存在时候才生效)
        Legend legend = mChart.getLegend();
        // 可以修改图表注解部分的位置
        // l.setPosition(LegendPosition.LEFT_OF_CHART);
        //线性也可以是圆
        legend.setForm(Legend.LegendForm.LINE);
        //设置标签文字的颜色
        legend.setTextColor(Color.BLUE);
        //获取X坐标轴
        XAxis xAxis = mChart.getXAxis();
        xAxis.setTextColor(Color.BLACK);
        //网格线
        xAxis.setDrawGridLines(false);
        //避免第一次最后剪裁
        xAxis.setAvoidFirstLastClipping(true);
        //几个X坐标之间才绘制
        xAxis.setSpaceBetweenLabels(1);
        //如果为false则X坐标不可见
        xAxis.setEnabled(true);
        //将X坐标轴放在底部，默认就是底部
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        //图表左边Y轴的坐标轴
        YAxis yAxis = mChart.getAxisLeft();
        //最大值
//        yAxis.setAxisMaxValue(100f);
        //最小值
        yAxis.setAxisMinValue(0);
        //不一定从零开始
        yAxis.setStartAtZero(false);
        //设置图表线
        yAxis.setDrawGridLines(true);

        //获取图表右边的坐标线
        YAxis right = mChart.getAxisRight();
        //不显示图表右边的坐标线
        right.setDrawGridLines(false);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn:

                addEntry(new Random().nextInt(1000));

                break;
        }

    }

    /**
     * 每次点击一次增加一个点
     * @param v 随机数
     */
    private void addEntry(int v) {
        LineData data = mChart.getData();
        //每个LineDataSet就代表一条线，每一个图表可以有很多条线，这些折线就像是数组一样，下标从零开始
        LineDataSet dataSet = data.getDataSetByIndex(0);
        //如果该折线图还没有数据集，就创建一个出来，反之则跳过此处
        if (dataSet == null) {
            dataSet = createDataSet();
            data.addDataSet(dataSet);
        }
        //先添加一个X坐标的值
        //因为是从零开始，data.getXValuesCount每次返回的都是X轴坐标上的总数量，所以不必多次一举加一
        data.addXValue(String.valueOf(data.getXValCount()));

        //dataSet.getEntryCount是获取所有统计图表上的数据点总量
        //从0开始的数组下标，不必多此一举加1
        Entry entry = new Entry((float) v,dataSet.getEntryCount());
        //往LineData里面添加点，addEntry第二个参数就代表折线的下标索引
        // 因为本例只有一个统计折线，那么就是第一个，其下标为0.
        // 如果同一张统计图表中存在若干条统计折线，那么必须分清是针对哪一条（依据下标索引）统计折线添加。
        data.addEntry(entry,0);
        //像ListView那样通知数据更新
        mChart.notifyDataSetChanged();
        //当前统计图表中X轴最多显示X轴坐标的总量
        mChart.setVisibleXRangeMaximum(10);

        // y坐标轴线最大值
//        mChart.setVisibleYRangeMaximum(30, YAxis.AxisDependency.LEFT);

        //将坐标移动到最新
        //此代码刷新图表的绘图
//        mChart.moveViewToX(data.getXValCount()-5);

        mChart.moveViewTo(data.getXValCount()-7, 55f,
                YAxis.AxisDependency.LEFT);
    }
    /**
     * 创建数据集
     * 初始化数据集，添加一条统计折线，可以简单的理解是初始化y坐标轴线上点的表征
     * @return LineDataSet
     */
    private LineDataSet createDataSet() {
        LineDataSet dataSet = new LineDataSet(null,"动态的添加数据");
        //设置轴的依赖
        dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        //设置折线的颜色,整体的蓝色
        dataSet.setColor(ColorTemplate.getHoloBlue());
        //包裹点的眼圈颜色
        dataSet.setCircleColor(Color.GREEN);
        //设置线的宽度
        dataSet.setLineWidth(2f);
        //设置填补透明
        dataSet.setFillAlpha(128);
        //设置填补的颜色
        dataSet.setFillColor(Color.WHITE);
        //设置高光的颜色
        dataSet.setHighLightColor(Color.BLACK);
        //设置文本值的颜色
        dataSet.setValueTextColor(Color.BLUE);
        //设置值的文本大小
        dataSet.setValueTextSize(12f);
        //设置显示的值
        dataSet.setDrawValues(true);
        return dataSet;
    }


}
