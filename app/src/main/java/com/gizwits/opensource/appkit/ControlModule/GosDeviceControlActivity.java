package com.gizwits.opensource.appkit.ControlModule;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.gizwits.gizwifisdk.api.GizWifiDevice;
import com.gizwits.gizwifisdk.enumration.GizWifiDeviceNetStatus;
import com.gizwits.gizwifisdk.enumration.GizWifiErrorCode;
import com.gizwits.opensource.appkit.R;
import com.gizwits.opensource.appkit.utils.HexStrUtils;
import com.gizwits.opensource.appkit.view.HexWatcher;


/*（整合）以下从bdmap移植过来
*
* */
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeOption;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import java.text.DecimalFormat;


public class GosDeviceControlActivity extends GosControlModuleBaseActivity
		implements OnClickListener, OnEditorActionListener,BDLocationListener{

	/**
	 * 设备列表传入的设备变量
	 */
	public String site1;
	private GizWifiDevice mDevice;
	private PoiSearch mPoiSearch;
	private TextView tv_data_gps_jingdu;
	private SeekBar sb_data_gps_jingdu;
	private TextView tv_data_gps_weidu;
	private SeekBar sb_data_gps_weidu;
	private TextView tv_data_gps_speed;

	/**
	 * （整合）百度地图相关变量
	 */
	private GeoCoder mGeoCoder;
	private MapView myMapView = null;//地图控件
	private BaiduMap myBaiduMap;//百度地图对象
	private LocationClient mylocationClient;//定位服务客户对象
	private MylocationListener mylistener;//重写的监听类
	private Context context;

	private double myLatitude;//纬度，用于存储自己所在位置的纬度
	private double myLongitude;//经度，用于存储自己所在位置的经度
	private float myCurrentX;

	private BitmapDescriptor myIconLocation1;//图标1，当前位置的箭头图标
	private MyOrientationListener myOrientationListener;//方向感应器类对象
	private MyLocationConfiguration.LocationMode locationMode;//定位图层显示方式
	private LinearLayout myLinearLayout1; //经纬度搜索区域1
	private LinearLayout myLinearLayout2; //地址搜索区域2


	private enum handler_key {

		/**
		 * 更新界面
		 */
		UPDATE_UI,

		DISCONNECT,
	}

	private Runnable mRunnable = new Runnable() {
		public void run() {
			if (isDeviceCanBeControlled()) {
				progressDialog.cancel();
			} else {
				toastDeviceNoReadyAndExit();
			}
		}

	};

	/**
	 * The handler.
	 */
	Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			handler_key key = handler_key.values()[msg.what];
			switch (key) {
				case UPDATE_UI:
					updateUI();
					break;
				case DISCONNECT:
					toastDeviceDisconnectAndExit();
					break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//(整合)
		SDKInitializer.initialize(getApplicationContext());
		setTheme(R.style.AppTheme);
		setContentView(R.layout.baidu_map);
		this.context = this;


		initDevice();
		//setActionBar(true, true, getDeviceName());
		initView();
		initLocation();
	}

	private void initView() {
		createSearch();
		tv_data_gps_jingdu = (TextView) findViewById(R.id.tv_data_gps_jingdu);
		tv_data_gps_weidu = (TextView) findViewById(R.id.tv_data_gps_weidu);
		tv_data_gps_speed = (TextView) findViewById(R.id.tv_data_gps_speed);


		//（整合）
		myMapView = (MapView) findViewById(R.id.baiduMapView);

		myBaiduMap = myMapView.getMap();


		//根据给定增量缩放地图级别
		MapStatusUpdate msu = MapStatusUpdateFactory.zoomTo(18.0f);
		myBaiduMap.setMapStatus(msu);
	}

	//（整合）
	private void initLocation() {
		locationMode = MyLocationConfiguration.LocationMode.NORMAL;

		//定位服务的客户端。宿主程序在客户端声明此类，并调用，目前只支持在主线程中启动
		mylocationClient = new LocationClient(this);
		mylistener = new MylocationListener();

		//注册监听器
		mylocationClient.registerLocationListener(mylistener);
		//配置定位SDK各配置参数，比如定位模式、定位时间间隔、坐标系类型等
		LocationClientOption mOption = new LocationClientOption();
		//设置坐标类型
		mOption.setCoorType("bd09ll");
		//设置是否需要地址信息，默认为无地址
		mOption.setIsNeedAddress(true);
		//设置是否打开gps进行定位
		mOption.setOpenGps(true);
		//设置扫描间隔，单位是毫秒 当<1000(1s)时，定时定位无效
		int span = 1000;
		mOption.setScanSpan(span);
		//设置 LocationClientOption
		mylocationClient.setLocOption(mOption);

		//初始化图标,BitmapDescriptorFactory是bitmap 描述信息工厂类.
		myIconLocation1 = BitmapDescriptorFactory.fromResource(R.drawable.location_marker);
//        myIconLocation2 = BitmapDescriptorFactory.fromResource(R.drawable.icon_target);

		//配置定位图层显示方式,三个参数的构造器
		MyLocationConfiguration configuration
				= new MyLocationConfiguration(locationMode, true, myIconLocation1);
		//设置定位图层配置信息，只有先允许定位图层后设置定位图层配置信息才会生效，参见 setMyLocationEnabled(boolean)
		myBaiduMap.setMyLocationConfigeration(configuration);

		myOrientationListener = new MyOrientationListener(context);
		//通过接口回调来实现实时方向的改变
		myOrientationListener.setOnOrientationListener(new MyOrientationListener.OnOrientationListener() {
			@Override
			public void onOrientationChanged(float x) {
				myCurrentX = x;
			}
		});

	}

	//(整合)
	/*
     *根据经纬度前往
     */
	public void getLocationByLL(double la, double lg) {
		//地理坐标的数据结构
		LatLng latLng = new LatLng(la, lg);
		//描述地图状态将要发生的变化,通过当前经纬度来使地图显示到该位置
		MapStatusUpdate msu = MapStatusUpdateFactory.newLatLng(latLng);
		myBaiduMap.setMapStatus(msu);
	}


	//(整合)
	  /*
     *定位请求回调接口
     */
	public class MylocationListener implements BDLocationListener {
		//定位请求回调接口
		private boolean isFirstIn = true;

		//定位请求回调函数,这里面会得到定位信息
		@Override
		public void onReceiveLocation(BDLocation bdLocation) {
			//BDLocation 回调的百度坐标类，内部封装了如经纬度、半径等属性信息
			//MyLocationData 定位数据,定位数据建造器
            /*
            * 可以通过BDLocation配置如下参数
            * 1.accuracy 定位精度
            * 2.latitude 百度纬度坐标
            * 3.longitude 百度经度坐标
            * 4.satellitesNum GPS定位时卫星数目 getSatelliteNumber() gps定位结果时，获取gps锁定用的卫星数
            * 5.speed GPS定位时速度 getSpeed()获取速度，仅gps定位结果时有速度信息，单位公里/小时，默认值0.0f
            * 6.direction GPS定位时方向角度
            * */
			myLatitude = bdLocation.getLatitude();
			myLongitude = bdLocation.getLongitude();
			MyLocationData data = new MyLocationData.Builder()
					.direction(myCurrentX)//设定图标方向
					.accuracy(bdLocation.getRadius())//getRadius 获取定位精度,默认值0.0f
					.latitude(myLatitude)//百度纬度坐标
					.longitude(myLongitude)//百度经度坐标
					.build();
			//设置定位数据, 只有先允许定位图层后设置数据才会生效，参见 setMyLocationEnabled(boolean)
			myBaiduMap.setMyLocationData(data);

			//判断是否为第一次定位,是的话需要定位到用户当前位置
			if (isFirstIn) {
				//根据当前所在位置经纬度前往
				getLocationByLL(myLatitude, myLongitude);
				isFirstIn = false;
				//提示当前所在地址信息
//                Toast.makeText(context, bdLocation.getAddrStr(), Toast.LENGTH_SHORT).show();
			}

		}
	}

	private void initDevice() {
		Intent intent = getIntent();
		mDevice = (GizWifiDevice) intent.getParcelableExtra("GizWifiDevice");
		mDevice.setListener(gizWifiDeviceListener);
		Log.i("Apptest", mDevice.getDid());
	}

	private String getDeviceName() {
		if (TextUtils.isEmpty(mDevice.getAlias())) {
			return mDevice.getProductName();
		}
		return mDevice.getAlias();
	}


	/*（整合）
      *定位服务的生命周期，达到节省
      */
	@Override
	protected void onStart() {
		super.onStart();
		//开启定位，显示位置图标
		myBaiduMap.setMyLocationEnabled(true);
		if (!mylocationClient.isStarted()) {
			mylocationClient.start();
		}
		myOrientationListener.start();
	}

	@Override
	protected void onStop() {
		super.onStop();
		//停止定位
		myBaiduMap.setMyLocationEnabled(false);
		mylocationClient.stop();
		myOrientationListener.stop();
	}

	@Override
	protected void onPause() {
		super.onPause();
		myMapView.onPause();
	}


	@Override
	protected void onResume() {
		super.onResume();

		getStatusOfDevice();
		//(整合)
		myMapView.onResume();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		//（整合）
		myMapView.onDestroy();


		mHandler.removeCallbacks(mRunnable);
		// 退出页面，取消设备订阅
		mDevice.setSubscribe(false);
		mDevice.setListener(null);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			default:
				break;
		}
	}

	/*
	 * ========================================================================
	 * EditText 点击键盘“完成”按钮方法
	 * ========================================================================
	 */
	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

		switch (v.getId()) {
			default:
				break;
		}
		hideKeyBoard();
		return false;

	}
	
	/*
	 * ========================================================================
	 * seekbar 回调方法重写
	 * ========================================================================
	 */


	/*
	 * ========================================================================
	 * 菜单栏
	 * ========================================================================
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.device_more, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

			case R.id.action_setDeviceInfo:
				setDeviceInfo();
				break;

			case R.id.action_getHardwareInfo:
				if (mDevice.isLAN()) {
					mDevice.getHardwareInfo();
				} else {
					myToast("只允许在局域网下获取设备硬件信息！");
				}
				break;

			case R.id.action_getStatu:
				mDevice.getDeviceStatus();
				break;

			default:
				break;
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * 添加maker
	 *
	 * @param
	 * @return
	 */
	private void setMaker(double la, double lg) {
		//定义坐标点
		LatLng point = new LatLng(la, lg);
		//构建maker图标
		BitmapDescriptor bitmap = BitmapDescriptorFactory.fromResource(R.drawable.icon_geo);
		//构建makeroption,用于在地图上添加maker
		OverlayOptions options = new MarkerOptions().position(point).icon(bitmap);
		//在地图上添加maker并显示出来
		myBaiduMap.addOverlay(options);
	}

	/**
	 * Description:根据保存的的数据点的值来更新UI
	 */
	protected void updateUI() {
		DecimalFormat df = new DecimalFormat("##0.000");
		EditText longtitude = (EditText) findViewById(R.id.editText_lg);
		EditText latitute = (EditText) findViewById(R.id.editText_la);
		EditText speed = (EditText) findViewById(R.id.editText_sp);
		EditText site = (EditText) findViewById(R.id.editText_site);
		data_gps_jingdu = data_gps_jingdu / 100000;
		data_gps_weidu = data_gps_weidu / 100000;
		data_gps_speed=xishu*data_gps_speed/100;
		LatLng centerLatLng =new LatLng(data_gps_weidu, data_gps_jingdu);
		mGeoCoder.reverseGeoCode(new ReverseGeoCodeOption().location(centerLatLng));
		final double mylg = data_gps_jingdu;
		final double myla = data_gps_weidu;
		longtitude.setText("东经"+data_gps_jingdu);
		latitute.setText("北纬" + data_gps_weidu);
		speed.setText(df.format(data_gps_speed)+"km/h");
		getLocationByLL(myla, mylg);
		setMaker(myla, mylg);
		site.setText(site1);



	}

	private void setEditText(EditText et, Object value) {
		et.setText(value.toString());
		et.setSelection(value.toString().length());
		et.clearFocus();
	}

	/**
	 * Description:页面加载后弹出等待框，等待设备可被控制状态回调，如果一直不可被控，等待一段时间后自动退出界面
	 */
	private void getStatusOfDevice() {
		// 设备是否可控
		if (isDeviceCanBeControlled()) {
			// 可控则查询当前设备状态
			mDevice.getDeviceStatus();
		} else {
			// 显示等待栏
			progressDialog.show();
			if (mDevice.isLAN()) {
				// 小循环10s未连接上设备自动退出
				mHandler.postDelayed(mRunnable, 10000);
			} else {
				// 大循环20s未连接上设备自动退出
				mHandler.postDelayed(mRunnable, 20000);
			}
		}
	}

	/**
	 * 发送指令,下发单个数据点的命令可以用这个方法
	 * <p>
	 * <h3>注意</h3>
	 * <p>
	 * 下发多个数据点命令不能用这个方法多次调用，一次性多次调用这个方法会导致模组无法正确接收消息，参考方法内注释。
	 * </p>
	 *
	 * @param key   数据点对应的标识名
	 * @param value 需要改变的值
	 */
	private void sendCommand(String key, Object value) {
		if (value == null) {
			return;
		}
		int sn = 5;
		ConcurrentHashMap<String, Object> hashMap = new ConcurrentHashMap<String, Object>();
		hashMap.put(key, value);
		// 同时下发多个数据点需要一次性在map中放置全部需要控制的key，value值
		// hashMap.put(key2, value2);
		// hashMap.put(key3, value3);
		mDevice.write(hashMap, sn);
		Log.i("liang", "下发命令：" + hashMap.toString());
	}

	private boolean isDeviceCanBeControlled() {
		return mDevice.getNetStatus() == GizWifiDeviceNetStatus.GizDeviceControlled;
	}

	private void toastDeviceNoReadyAndExit() {
		Toast.makeText(this, "设备无响应，请检查设备是否正常工作", Toast.LENGTH_LONG).show();
		finish();
	}

	private void toastDeviceDisconnectAndExit() {
		Toast.makeText(GosDeviceControlActivity.this, "连接已断开", Toast.LENGTH_SHORT).show();
		finish();
	}

	/**
	 * 展示设备硬件信息
	 *
	 * @param hardwareInfo
	 */
	private void showHardwareInfo(String hardwareInfo) {
		String hardwareInfoTitle = "设备硬件信息";
		new AlertDialog.Builder(this).setTitle(hardwareInfoTitle).setMessage(hardwareInfo)
				.setPositiveButton(R.string.besure, null).show();
	}

	/**
	 * Description:设置设备别名与备注
	 */
	private void setDeviceInfo() {

		final Dialog mDialog = new AlertDialog.Builder(this).setView(new EditText(this)).create();
		mDialog.show();

		Window window = mDialog.getWindow();
		window.setContentView(R.layout.alert_gos_set_device_info);

		final EditText etAlias;
		final EditText etRemark;
		etAlias = (EditText) window.findViewById(R.id.etAlias);
		etRemark = (EditText) window.findViewById(R.id.etRemark);

		LinearLayout llNo, llSure;
		llNo = (LinearLayout) window.findViewById(R.id.llNo);
		llSure = (LinearLayout) window.findViewById(R.id.llSure);

		if (!TextUtils.isEmpty(mDevice.getAlias())) {
			setEditText(etAlias, mDevice.getAlias());
		}
		if (!TextUtils.isEmpty(mDevice.getRemark())) {
			setEditText(etRemark, mDevice.getRemark());
		}

		llNo.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mDialog.dismiss();
			}
		});

		llSure.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (TextUtils.isEmpty(etRemark.getText().toString())
						&& TextUtils.isEmpty(etAlias.getText().toString())) {
					myToast("请输入设备别名或备注！");
					return;
				}
				mDevice.setCustomInfo(etRemark.getText().toString(), etAlias.getText().toString());
				mDialog.dismiss();
				String loadingText = (String) getText(R.string.loadingtext);
				progressDialog.setMessage(loadingText);
				progressDialog.show();
			}
		});

		mDialog.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				hideKeyBoard();
			}
		});
	}

	/*
	 * 获取设备硬件信息回调
	 */
	@Override
	protected void didGetHardwareInfo(GizWifiErrorCode result, GizWifiDevice device,
									  ConcurrentHashMap<String, String> hardwareInfo) {
		super.didGetHardwareInfo(result, device, hardwareInfo);
		StringBuffer sb = new StringBuffer();
		if (GizWifiErrorCode.GIZ_SDK_SUCCESS != result) {
			myToast("获取设备硬件信息失败：" + result.name());
		} else {
			sb.append("Wifi Hardware Version:" + hardwareInfo.get(WIFI_HARDVER_KEY) + "\r\n");
			sb.append("Wifi Software Version:" + hardwareInfo.get(WIFI_SOFTVER_KEY) + "\r\n");
			sb.append("MCU Hardware Version:" + hardwareInfo.get(MCU_HARDVER_KEY) + "\r\n");
			sb.append("MCU Software Version:" + hardwareInfo.get(MCU_SOFTVER_KEY) + "\r\n");
			sb.append("Wifi Firmware Id:" + hardwareInfo.get(WIFI_FIRMWAREID_KEY) + "\r\n");
			sb.append("Wifi Firmware Version:" + hardwareInfo.get(WIFI_FIRMWAREVER_KEY) + "\r\n");
			sb.append("Product Key:" + "\r\n" + hardwareInfo.get(PRODUCT_KEY) + "\r\n");

			// 设备属性
			sb.append("Device ID:" + "\r\n" + mDevice.getDid() + "\r\n");
			sb.append("Device IP:" + mDevice.getIPAddress() + "\r\n");
			sb.append("Device MAC:" + mDevice.getMacAddress() + "\r\n");
		}
		showHardwareInfo(sb.toString());
	}

	/*
	 * 设置设备别名和备注回调
	 */
	@Override
	protected void didSetCustomInfo(GizWifiErrorCode result, GizWifiDevice device) {
		super.didSetCustomInfo(result, device);
		if (GizWifiErrorCode.GIZ_SDK_SUCCESS == result) {
			myToast("设置成功");
			progressDialog.cancel();
			finish();
		} else {
			myToast("设置失败：" + result.name());
		}
	}

	/*
	 * 设备状态改变回调，只有设备状态为可控才可以下发控制命令
	 */
	@Override
	protected void didUpdateNetStatus(GizWifiDevice device, GizWifiDeviceNetStatus netStatus) {
		super.didUpdateNetStatus(device, netStatus);
		if (netStatus == GizWifiDeviceNetStatus.GizDeviceControlled) {
			mHandler.removeCallbacks(mRunnable);
			progressDialog.cancel();
		} else {
			mHandler.sendEmptyMessage(handler_key.DISCONNECT.ordinal());
		}
	}

	/*
	 * 设备上报数据回调，此回调包括设备主动上报数据、下发控制命令成功后设备返回ACK
	 */
	@Override
	protected void didReceiveData(GizWifiErrorCode result, GizWifiDevice device,
								  ConcurrentHashMap<String, Object> dataMap, int sn) {
		super.didReceiveData(result, device, dataMap, sn);
		Log.i("liang", "接收到数据");
		if (result == GizWifiErrorCode.GIZ_SDK_SUCCESS && dataMap.get("data") != null) {
			getDataFromReceiveDataMap(dataMap);
			mHandler.sendEmptyMessage(handler_key.UPDATE_UI.ordinal());
		}
	}


	@Override
	public void onReceiveLocation(BDLocation bdLocation) {
		StringBuilder currentPosition = new StringBuilder();
		currentPosition.append(bdLocation.getCountry());
		EditText site = (EditText) findViewById(R.id.editText_site);
		site.setText(currentPosition);
	}







	/**
	 * 检索 创建
	 */
	public void createSearch() {

		//兴趣点检索   没有用到
		mPoiSearch = PoiSearch.newInstance();
		OnGetPoiSearchResultListener poiListener = new OnGetPoiSearchResultListener() {
			@Override
			public void onGetPoiResult(PoiResult result) {
				//获取POI检索结果

			}

			@Override
			public void onGetPoiDetailResult(PoiDetailResult result) {
				//获取Place详情页检索结果
			}

		};
		//mPoiSearch.searchInCity((new PoiCitySearchOption()).city(“北京”).keyword(“美食”).pageNum(10)).pageNum(10));
		mPoiSearch.setOnGetPoiSearchResultListener(poiListener);
		//地里编码
		mGeoCoder = GeoCoder.newInstance();
		OnGetGeoCoderResultListener getGeoListener = new OnGetGeoCoderResultListener() {
			@Override
			public void onGetGeoCodeResult(GeoCodeResult result) {
				if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
					//没有检索到结果
				}
				//获取地理编码结果
			}

			@Override
			public void onGetReverseGeoCodeResult(ReverseGeoCodeResult result) {
				if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
					//没有找到检索结果
				}
				//设置搜索地址
				PoiInfo userPoi = new PoiInfo();
				userPoi.location = result.getLocation();
				userPoi.address = result.getAddress();
				site1 = userPoi.address;

			}
		};
		mGeoCoder.setOnGetGeoCodeResultListener(getGeoListener);
	}

}





