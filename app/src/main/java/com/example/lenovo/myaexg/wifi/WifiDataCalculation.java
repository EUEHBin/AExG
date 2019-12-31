package com.example.lenovo.myaexg.wifi;

import android.os.Message;
import android.util.Log;
import android.widget.Button;

import com.example.lenovo.myaexg.R;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Eueh on 2019/9/16.
 * www.meueh.com
 */

public class WifiDataCalculation {
    // Debugging
    private static final String TAG = "WifiDataCalculation";
    /* Packet construction 包构造 */
    private static final int PACKET_ID_LEN = 1;	/* packet number length 包号长度  */
    private static final int STATUS_BYTES_LEN = 3; //状态字节长度
    private static final int CHANNELS = 2; //渠道
    private static final int BYTES_PER_CHANNEL = 3; //每渠道字节
    //最大包字节数
    private static final int MAX_NUM_PACKET_BYTES = PACKET_ID_LEN + STATUS_BYTES_LEN + (CHANNELS * BYTES_PER_CHANNEL);

    /* Data conversion states  数据转换状态 */
    private static int data_state; //数据状态---int的默认值是0
    private static final int INVALID_DATA = 0; //无效数据
    private static final int FIRST_START_BYTE = 1; //初始字节
    private static final int VALID_DATA = 3; //有效数据

    /* Data conversion variables  数据转换变量 */
    private static int packet_count = 0; //包计数
    private static int byte_count = 0; //字节计数
    private static int previous_packet_number = 0; //"以前"包数量
    private static int current_x_value = 0; //当前x数值
    private static int update_count = 0; //更新计数
    static int ch = 0;

    /* Sampling Constants  采样常数 */
    private static final int SPS = 250;
    private static final double Fs = (double) SPS;
    private static final double Ts = 1 / Fs;

    /* LPF Cut-off Variables  低通滤波器截止变量 */
    private static final double[] LPF1_options = {0, 8.0, 10.0, 15.0, 10000.0};//低通滤波器1选项
    private static int LPF1_options_ptr = 2; //低通滤波器1选项记录
    private static double LPF1_Fc = LPF1_options[LPF1_options_ptr];//低通滤波器截止频率
    private static double LPF1_Tc = 1 / LPF1_Fc; //低通滤波器TC
    private static double LPF1_RC = LPF1_Tc / (2 * Math.PI);//低通滤波器RC
    private static double LPF1_ALPHA = Ts / (LPF1_RC + Ts);

    private static final double[] LPF2_options = {0, 8.0, 10.0, 15.0, 10000.0};
    private static int LPF2_options_ptr = 2;
    private static double LPF2_Fc = LPF2_options[LPF2_options_ptr];
    private static double LPF2_Tc = 1 / LPF2_Fc;
    private static double LPF2_RC = LPF2_Tc / (2 * Math.PI);
    private static double LPF2_ALPHA = Ts / (LPF2_RC + Ts);

    /* HPF Cut-off Variables  高通滤波器截止变量 */
    private static final double[] HPF1_options = {0, 0.1, 0.5, 1.5, 5.0};
    private static int HPF1_options_ptr = 3;
    private static double HPF1_Fc = HPF1_options[HPF1_options_ptr]; //TODO: LPF_options ??
    private static double HPF1_Tc = 1 / HPF1_Fc;
    private static double HPF1_RC = HPF1_Tc / (2 * Math.PI);
    private static double HPF1_ALPHA = HPF1_RC / (HPF1_RC + Ts);

    private static final double[] HPF2_options = {0, 0.1, 0.5, 1.5, 5.0};
    private static int HPF2_options_ptr = 3;
    private static double HPF2_Fc = HPF2_options[HPF2_options_ptr]; //TODO: LPF_options ??
    private static double HPF2_Tc = 1 / HPF2_Fc;
    private static double HPF2_RC = HPF2_Tc / (2 * Math.PI);
    private static double HPF2_ALPHA = HPF2_RC / (HPF2_RC + Ts);

    /* Filter Variables   过滤变量 */
    private static double LPF1_filter_value = 0;
    private static double HPF1_filter_value = 0;
    private static double[] filter_input1 = {0, 0};
    private static double LPF2_filter_value = 0;
    private static double HPF2_filter_value = 0;
    private static double[] filter_input2 = {0, 0};
    private static boolean LPF1_on = true;
    private static boolean HPF1_on = true;
    private static boolean LPF2_on = true;
    private static boolean HPF2_on = true;

    /* Channel Parameters  通道参数*/
    private static final int CHANNEL_GAIN = 6;
    private static final double VOLTAGE_RANGE = 2.4; //电压范围
    private static final double VOLTAGE_DIVISOR = (VOLTAGE_RANGE / (0x7FFFFF * CHANNEL_GAIN)) * 1000;

    /* Debug variables  调试变量 */
    private static int previous_ch = 0;


    //mFCLow_Button1
    static void mFCLowButton1(Button mFCLow_Button1) {
        HPF1_options_ptr = (++HPF1_options_ptr) % HPF1_options.length;
        switch (HPF1_options_ptr) {
            case 0:
                mFCLow_Button1.setText(R.string.fclow_none);
                break;
            case 1:
                mFCLow_Button1.setText(R.string.fclow_0_1);
                break;
            case 2:
                mFCLow_Button1.setText(R.string.fclow_0_5);
                break;
            case 3:
                mFCLow_Button1.setText(R.string.fclow_1_5);
                break;
            case 4:
                mFCLow_Button1.setText(R.string.fclow_5);
                break;
        }
        HPF1_Filter_Config(HPF1_options[HPF1_options_ptr]);
    }

    //mFCHigh_Button1
    public static void mFCHighButton1(Button mFCHigh_Button1) {
        LPF1_options_ptr = (++LPF1_options_ptr) % LPF1_options.length;
        switch (LPF1_options_ptr) {
            case 0:
                mFCHigh_Button1.setText(R.string.fchigh_none);
                break;
            case 1:
                mFCHigh_Button1.setText(R.string.fchigh_8);
                break;
            case 2:
                mFCHigh_Button1.setText(R.string.fchigh_10);
                break;
            case 3:
                mFCHigh_Button1.setText(R.string.fchigh_15);
                break;
            case 4:
                mFCHigh_Button1.setText(R.string.fchigh_10k);
                break;
        }
        LPF1_Filter_Config(LPF1_options[LPF1_options_ptr]);
    }

    //mFCLow_Button2
    static void mFCLowButton2(Button mFCLow_Button2) {
        HPF2_options_ptr = (++HPF2_options_ptr) % HPF2_options.length;
        switch (HPF2_options_ptr) {
            case 0:
                mFCLow_Button2.setText(R.string.fclow_none);
                break;
            case 1:
                mFCLow_Button2.setText(R.string.fclow_0_1);
                break;
            case 2:
                mFCLow_Button2.setText(R.string.fclow_0_5);
                break;
            case 3:
                mFCLow_Button2.setText(R.string.fclow_1_5);
                break;
            case 4:
                mFCLow_Button2.setText(R.string.fclow_5);
                break;
        }
        HPF2_Filter_Config(HPF2_options[HPF2_options_ptr]);
    }

    //mFCHigh_Button2
    static void mFCHighButton2(Button mFCHigh_Button2) {
        LPF2_options_ptr = (++LPF2_options_ptr) % LPF2_options.length;
        switch (LPF2_options_ptr) {
            case 0:
                mFCHigh_Button2.setText(R.string.fchigh_none);
                break;
            case 1:
                mFCHigh_Button2.setText(R.string.fchigh_8);
                break;
            case 2:
                mFCHigh_Button2.setText(R.string.fchigh_10);
                break;
            case 3:
                mFCHigh_Button2.setText(R.string.fchigh_15);
                break;
            case 4:
                mFCHigh_Button2.setText(R.string.fchigh_10k);
                break;
        }
        LPF2_Filter_Config(LPF2_options[LPF2_options_ptr]);
    }

    private static void LPF1_Filter_Config(double fc) {
        Log.d(TAG, "HFc: " + fc);
        if (fc == 0) {
            LPF1_on = false;
        } else {
            LPF1_on = true;
            LPF1_Fc = fc;
            LPF1_Tc = 1 / LPF1_Fc;
            LPF1_RC = LPF1_Tc / (2 * Math.PI);
            LPF1_ALPHA = Ts / (LPF1_RC + Ts);
        }
    }

    private static void HPF1_Filter_Config(double fc) {
        if (fc == 0) {
            HPF1_on = false;
        } else {
            HPF1_on = true;
            HPF1_Fc = fc;
            HPF1_Tc = 1 / HPF1_Fc;
            HPF1_RC = HPF1_Tc / (2 * Math.PI);
            HPF1_ALPHA = HPF1_RC / (HPF1_RC + Ts);
        }
    }

    private static void LPF2_Filter_Config(double fc) {
        Log.d(TAG, "HFc: " + fc);
        if (fc == 0) {
            LPF2_on = false;
        } else {
            LPF2_on = true;
            LPF2_Fc = fc;
            LPF2_Tc = 1 / LPF2_Fc;
            LPF2_RC = LPF2_Tc / (2 * Math.PI);
            LPF2_ALPHA = Ts / (LPF2_RC + Ts);
        }
    }

    private static void HPF2_Filter_Config(double fc) {
        if (fc == 0) {
            HPF2_on = false;
        } else {
            HPF2_on = true;
            HPF2_Fc = fc;
            HPF2_Tc = 1 / HPF2_Fc;
            HPF2_RC = HPF2_Tc / (2 * Math.PI);
            HPF2_ALPHA = HPF2_RC / (HPF2_RC + Ts);
        }
    }

    public static void changeData(byte[] str, int length) {

        Log.d("MyAexgTag","str:"+str+"\n"+"length:"+length);

        // *******************************************
        // Data from BT comes in here, then needs to be
        // placed in a buffer ready for the graphings
        //来自BT的数据来自这里，然后需要放置在准备好图形的缓冲区中
        //********************************************
        int data;
        ArrayList<Integer> list = new ArrayList<Integer>();

        int x, y;

        byte[] readBuf = (byte[]) str;

        Log.d("MyAexgTag","readBuf:"+ Arrays.toString(readBuf).length());

        for (int i = 0; i < length; i++) {
            //byte为8bit，转成int会自动补齐高位全为1，为了保证数据的一致性
            //需要和 0xFF 做 按位与运算
            // 如字节为 10100101 转成int后，总长度为32，会自动前面补位为1
            //为了保证数据正常，需要做 & 运算（全为1才是1），保证数据正确性
            data = (int) readBuf[i] & 0xFF;
            list.add(data);
            //INVALID_DATA = 0; //无效数据
            //FIRST_START_BYTE = 1; //初始字节
            //VALID_DATA = 3; //有效数据
            switch (data_state) {
                case INVALID_DATA:
                    if (data == 170) {
                        data_state = FIRST_START_BYTE;
                    }
                    break;
                case FIRST_START_BYTE:
                    if (data == 85) {
                        data_state = VALID_DATA;
                        packet_count = 0;
                        byte_count = 0;
                        ch = 0;
                    } else {
                        data_state = INVALID_DATA;
                    }
                    break;
                case VALID_DATA:
                    if (packet_count == 0) {
                        getx(data);
                    } else if ((packet_count > 3) && (packet_count < MAX_NUM_PACKET_BYTES)) {
                        ch <<= 8;
                        ch += data;
                        byte_count++;
                        if ((byte_count % 3) == 0) {
                            ch = ch & 0x00FFFFFF;
                            ch = twos(ch);

                            plot(current_x_value, ch, byte_count / 3);
                            ch = 0;
                        }
                    }
                    packet_count++;
                    if (packet_count >= MAX_NUM_PACKET_BYTES) {
//                        updateChart();
                        data_state = INVALID_DATA;
                        packet_count = 0;
                        byte_count = 0;
                    }
                    break;
            }
        }
        previous_ch = ch;
    }

    private static void getx(int packet_num) {
        if (packet_num < previous_packet_number) {
            current_x_value += (256 - previous_packet_number) + packet_num;
        } else {
            current_x_value += (packet_num - previous_packet_number);
        }
        previous_packet_number = packet_num;
    }

    private static int twos(int num) {
        if (num >= 0x800000) {
            num = 0xFF000000 + num;
        }
        return num;
    }

    public static double to_voltage(double value) {
        return value * VOLTAGE_DIVISOR;
    }

    public static double to_time(double value) {
        return value * Ts;
    }

    public static void plot(int x, int y, int channel) {
        double localx, localy;
//        mCurrentSeries = mDataset.getSeriesAt(channel - 1);
        localx = to_time((double) x);
        localy = to_voltage((double) y);

        if (channel == 1) {
            localy = HPF1(LPF1(localy));
        } else if (channel == 2) {
            localy = HPF2(LPF2(localy)) + 2;
        }
//        mCurrentSeries.add(localx, localy);
//        if ((mCurrentSeries.getMaxX() - mCurrentSeries.getMinX()) > (to_time((double) mWindowSize - 1))) {
//            mCurrentSeries.remove(0);
//        }
    }

    public static double HPF1(double raw) {
        if (HPF1_on) {
            HPF1_filter_value = HPF1_ALPHA * (HPF1_filter_value + raw - filter_input1[0]);
            filter_input1[0] = raw; // TODO: raw?
            return HPF1_filter_value;
        } else {
            return raw;
        }
    }

    public static double LPF1(double raw) {
        if (LPF1_on) {
            LPF1_filter_value = LPF1_filter_value + (LPF1_ALPHA * (raw - LPF1_filter_value));
            return LPF1_filter_value;
        } else {
            return raw;
        }
    }


    public static double LPF2(double raw) {
        if (LPF2_on) {
            LPF2_filter_value = LPF2_filter_value + (LPF2_ALPHA * (raw - LPF2_filter_value));
            return LPF2_filter_value;
        } else {
            return raw;
        }
    }

    public static double HPF2(double raw) {
        if (HPF2_on) {
            HPF2_filter_value = HPF2_ALPHA * (HPF2_filter_value + raw - filter_input2[0]);
            filter_input2[0] = raw; // TODO: raw?
            return HPF2_filter_value;
        } else {
            return raw;
        }
    }

//    public static void updateChart() {
//        double margin = 0;
//        if (mChartView != null) {
//            if (autofit_on) {
//                margin = (mCurrentSeries.getMaxY() - mCurrentSeries.getMinY()) * 0.2;
//                mRenderer.setYAxisMax(mCurrentSeries.getMaxY() + margin);
//                mRenderer.setYAxisMin(mCurrentSeries.getMinY() - margin);
//            }
//            mChartView.invalidate();
//            update_count = 0;
//        }
//    }

}
