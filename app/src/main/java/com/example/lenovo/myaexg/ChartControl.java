package com.example.lenovo.myaexg;

import android.graphics.Color;

import org.achartengine.GraphicalView;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.util.ArrayList;


public class ChartControl {
/* Filter */
	/* Sampling Constants */
	private static final int SPS = 250;
	private static final double Fs = (double)SPS;
	private static final double Ts = 1/Fs;
	
	/* LPF Cut-off Variables */
	private static final double[] LPF_options = {0, 8.0, 10.0, 15.0,10000.0};
	private static int LPF_options_ptr = 2;
	private static double LPF_Fc = LPF_options[LPF_options_ptr];
	private static double LPF_Tc = 1/LPF_Fc;
	private static double LPF_RC = LPF_Tc/(2* Math.PI);
	private static double LPF_ALPHA = Ts/(LPF_RC+Ts);
	
	/* HPF Cut-off Variables */
	private static final double[] HPF_options = {0, 0.1,0.5, 1.5, 5.0};
	private static int HPF_options_ptr = 3;
	private static double HPF_Fc = LPF_options[HPF_options_ptr];
	private static double HPF_Tc = 1/HPF_Fc;
	private static double HPF_RC = HPF_Tc/(2* Math.PI);
	private static double HPF_ALPHA = HPF_RC/(HPF_RC+Ts);
	
	/* Filter Variables */
	private static double LPF_filter_value = 0;
	private static double HPF_filter_value = 0;
	private static double[] filter_input = {0,0};
	private static boolean LPF_on = true;
	private static boolean HPF_on = true;
	
	
	/* Channel Parameters */
	private static final int CHANNEL_GAIN = 6;
	private static final double VOLTAGE_RANGE = 2.4;
	private static final double VOLTAGE_DIVISOR = (VOLTAGE_RANGE/(0x7FFFFF*CHANNEL_GAIN))*1000;
	
/* Chart */
	private int number_of_channels = 1;
	
	private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
    private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
    private XYSeries mCurrentSeries;
    private ArrayList<XYSeries> mArraySeries;
    private XYSeriesRenderer mCurrentRenderer;
    private GraphicalView mChartView;
    
    private boolean static_plot = true;
    private int plotter_pointer = 0;
    
    
/* Colours */
    private int[] mColour = {
    		Color.BLUE, Color.RED,
    		Color.CYAN, Color.GREEN,
    		Color.MAGENTA, Color.YELLOW,
    		Color.GRAY, Color.BLACK};
	
	public ChartControl(){
		
		/***********************************
         * Chart setup
         **********************************/
//        setContentView(R.layout.main);
        //mX = (EditText) findViewById(R.id.xValue);
        //mY = (EditText) findViewById(R.id.yValue);
        mRenderer.setApplyBackgroundColor(true);
        mRenderer.setBackgroundColor(Color.WHITE);
        mRenderer.setMarginsColor(Color.WHITE);
        mRenderer.setLabelsColor(Color.BLUE);
        mRenderer.setAxisTitleTextSize(16);
        mRenderer.setChartTitleTextSize(20);
        mRenderer.setLabelsTextSize(15);
        mRenderer.setLegendTextSize(15);
        mRenderer.setMargins(new int[] { 20, 30, 15, 0 });
        mRenderer.setZoomButtonsVisible(false);
        mRenderer.setPointSize(1);
        mRenderer.setYAxisMax(3);
        mRenderer.setYAxisMin(-1);
        mRenderer.setShowGrid(true);
//        mRenderer.setXLabels((mWindowSize*5)/SPS);
        mRenderer.setShowLegend(false);
        mRenderer.setShowLabels(false);
        mRenderer.setXLabelsAngle(90);
        mRenderer.setYTitle("mV");
        
     // Create plot with axis
        for (int i=0;i<2;i++){
	        String seriesTitle = "Channel " + (mDataset.getSeriesCount() + 1);// + (mDataset.getSeriesCount() + 1);
	        XYSeries series = new XYSeries(seriesTitle);
	        mDataset.addSeries(series);
	       // mCurrentSeries = series;
	        XYSeriesRenderer renderer = new XYSeriesRenderer();
	        mRenderer.addSeriesRenderer(renderer);
	        //renderer.setPointStyle(PointStyle.CIRCLE);
	        //renderer.setFillPoints(true);
	        mCurrentRenderer = renderer;
	        mCurrentRenderer.setColor(mColour[i]);
        }
        
        mCurrentSeries = mDataset.getSeriesAt(0);
		
	}
}
