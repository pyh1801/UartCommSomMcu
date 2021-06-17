/**
 * Copyright (c) 2014-2016, Digi International Inc. <support@digi.com>
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.digi.android.sample.serialconsole;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.digi.android.serial.ISerialPortEventListener;
import com.digi.android.serial.NoSuchPortException;
import com.digi.android.serial.PortInUseException;
import com.digi.android.serial.SerialPort;
import com.digi.android.serial.SerialPortEvent;
import com.digi.android.serial.SerialPortManager;
import com.digi.android.serial.SerialPortParameters;
import com.digi.android.serial.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TooManyListenersException;

public class MainActivity extends Activity implements ISerialPortEventListener {
	Button btn_menu, btn_conn;
	TextView txt_recvMsg;

	SerialPortManager manager;
	String[] serialPortsList;
	SerialPort[] serialPorts;
	SerialPortParameters param;
	String serialPortName;
	SerialPort serialPort;

	InputStream inputStream;
	OutputStream outputStream;


	boolean STATUS_CONN = false;

	boolean isRecv = false;
	Thread t;

	TextView txt_isRecv;
	StringBuilder sb;
	String s;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		manager = new SerialPortManager(this);
		Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
		initUI();
		search();
		config();

	}

	class ExceptionHandler implements Thread.UncaughtExceptionHandler {

		@Override
		public void uncaughtException(Thread t, Throwable e) {
			e.printStackTrace();
			close();
//			android.os.Process.killProcess(android.os.Process.myPid());
//			System.exit(10);

		}
	}


	@Override
	public void serialEvent(SerialPortEvent event) {
		switch(event.getEventType()) {
			case DATA_AVAILABLE:
				try {
					if(inputStream.available() > 0) {
						recv();
						txt_isRecv.setText(sb.toString());
					}
				} catch (Exception e) {
					close();
				}
				break;
		}
	}

	private void initUI() {
		btn_conn = findViewById(R.id.btn_conn);
		btn_conn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(STATUS_CONN) { // serialPort 연결 상태일 경우
					close(); // 연결 해제
				} else {
					connect();
				}
			}
		});

		btn_menu = findViewById(R.id.btn_menu);
		/** 사용자가 메뉴 버튼 터치 시, MCU로 명령 패킷이 전송 */
		btn_menu.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				send();
			}
		});

		txt_recvMsg = findViewById(R.id.txt_recvMsg);
		txt_isRecv = findViewById(R.id.txt_isRecv);
	}

	private void search() {
		serialPortsList = manager.listSerialPorts();
		serialPorts = new SerialPort[serialPortsList.length];

		for(int i=0; i<serialPortsList.length; i++) {
			if(serialPortsList[i].equals("/dev/ttyLP0")) {
				serialPortName = serialPortsList[i];
				serialPort = serialPorts[i];
				Log.e("search", "This serial Port: " + serialPortsList[i]);
			}
			Log.e("search", "" + serialPortsList[i]);
		}


	}

	private void config() {
		param = new SerialPortParameters(
				9600,
				SerialPort.DATABITS_8,
				SerialPort.STOPBITS_1,
				SerialPort.PARITY_NONE,
				SerialPort.FLOWCONTROL_NONE,
				2000);
	}

	private void connect() {
		STATUS_CONN = true;

		try {
			Log.e("connect()", "socket opening");
			serialPort = manager.openSerialPort(serialPortName, param);
			serialPort.registerEventListener(this);
			serialPort.notifyOnDataAvailable(true);

			inputStream = serialPort.getInputStream();
			outputStream = serialPort.getOutputStream();

		} catch (TooManyListenersException e) {
			serialPort.close();
		} catch (PortInUseException e) {
			serialPort.close();

		} catch (NoSuchPortException e) {
			serialPort.close();

		} catch (UnsupportedCommOperationException e) {
			serialPort.close();

		} catch (IOException e) {
			serialPort.close();
		}
	}

	private void send() {
		try {
//			String cmdPacket = "SoM Send ";
			String cmdPacket = "VR";

			Log.e("send()", "Message sending");
			outputStream.write(cmdPacket.getBytes());
			Log.e("send()", "Message sent");
			outputStream.flush();
			outputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void recv() {
		try {
			int streamSize = inputStream.available();

			if(streamSize > 0) {
				byte[] buf = new byte[streamSize];
				Log.e("recv()", "Message receiving");
				int recvMsgSize = inputStream.read(buf, 0, streamSize);

				if(recvMsgSize <= 0) {
					Log.e("recv()", "Message none");
					return;
				}

				txt_recvMsg.setText(new String(buf));
				Log.e("recv()", "Received message: " + new String(buf));

			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

//	private void recv() {
////		isRecv = true;
//
//		t = new Thread() {
//
//			@Override
//			public void run() {
////				while(isRecv) {
//					try {
//						sb = new StringBuilder();
//
//						int streamSize = inputStream.available();
//
//						if(streamSize > 0) {
//							byte[] buf = new byte[streamSize];
//
//							Log.e("recv()", "Message receiving");
//							int recvMsgSize = inputStream.read(buf, 0, streamSize);
//
//							if(recvMsgSize <= 0) {
//								Log.e("recv()", "Message none");
//								return;
//							}
//
////							txt_recvMsg.setText(new String(buf));
//							Log.e("recv()", "Received message: " + new String(buf));
//
//							s = new String(buf);
//							sb.append(new String(buf));
//
////							Thread.sleep(3000);
//
//							close();
//						}
//
//					} catch (IOException e) {
//						e.printStackTrace();
//					}
////				} // while
//			}
//		};
//		t.start();
//
//	}

	private void close() {
		STATUS_CONN = false;

		if(serialPort != null) {
			serialPort.close();
			Log.e("close()", "socket close");
		}

		serialPort = null;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Toast.makeText(this, "onDestroy 호출 됨", Toast.LENGTH_LONG).show();
		close();
	}
}






// 백업(20.10.08)
///**
// * Copyright (c) 2014-2016, Digi International Inc. <support@digi.com>
// *
// * Permission to use, copy, modify, and/or distribute this software for any
// * purpose with or without fee is hereby granted, provided that the above
// * copyright notice and this permission notice appear in all copies.
// *
// * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
// * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
// * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
// * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
// * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
// * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
// * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
// */
//
//package com.digi.android.sample.serialconsole;
//
//		import android.app.Activity;
//		import android.os.Bundle;
//		import android.util.Log;
//		import android.view.View;
//		import android.widget.Button;
//		import android.widget.TextView;
//
//		import com.digi.android.serial.ISerialPortEventListener;
//		import com.digi.android.serial.NoSuchPortException;
//		import com.digi.android.serial.PortInUseException;
//		import com.digi.android.serial.SerialPort;
//		import com.digi.android.serial.SerialPortEvent;
//		import com.digi.android.serial.SerialPortManager;
//		import com.digi.android.serial.SerialPortParameters;
//		import com.digi.android.serial.UnsupportedCommOperationException;
//
//		import java.io.IOException;
//		import java.io.InputStream;
//		import java.io.OutputStream;
//		import java.util.TooManyListenersException;
//
//public class MainActivity extends Activity implements ISerialPortEventListener {
//	Button btn_menu, btn_conn;
//	TextView txt_recvMsg;
//
//	SerialPortManager manager;
//	String[] serialPortsList;
//	SerialPort[] serialPorts;
//	SerialPortParameters param;
//	String serialPortName;
//	SerialPort serialPort;
//
//	InputStream inputStream;
//	OutputStream outputStream;
//
//	boolean STATUS_CONN = false;
//
//	@Override
//	public void onCreate(Bundle savedInstanceState) {
//		super.onCreate(savedInstanceState);
//		setContentView(R.layout.activity_main);
//
//		manager = new SerialPortManager(this);
//
//		initUI();
//		search();
//		config();
//		connect();
//	}
//
//	private void initUI() {
//		btn_conn = findViewById(R.id.btn_conn);
//		btn_conn.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				if(STATUS_CONN) { // serialPort 연결 상태일 경우
//					close(); // 연결 해제
//				} else {
//					connect();
//				}
//			}
//		});
//
//		btn_menu = findViewById(R.id.btn_menu);
//		/** 사용자가 메뉴 버튼 터치 시, MCU로 명령 패킷이 전송 */
//		btn_menu.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				send();
//			}
//		});
//
//		txt_recvMsg = findViewById(R.id.txt_recvMsg);
//	}
//
//	private void search() {
//		serialPortsList = manager.listSerialPorts();
//		serialPorts = new SerialPort[serialPortsList.length];
//
//		for(int i=0; i<serialPortsList.length; i++) {
//			if(serialPortsList[i].equals("/dev/ttyLP0")) {
//				serialPortName = serialPortsList[i];
//				serialPort = serialPorts[i];
//				Log.e("search", "This serial Port: " + serialPortsList[i]);
//			}
////			Log.e("search", "" + serialPortsList[i]);
//		}
//	}
//
//	private void config() {
//		param = new SerialPortParameters(
//				9600,
//				SerialPort.DATABITS_8,
//				SerialPort.STOPBITS_1,
//				SerialPort.PARITY_NONE,
//				SerialPort.FLOWCONTROL_NONE,
//				2000);
//	}
//
//	private void connect() {
//		STATUS_CONN = true;
//
//		try {
//			Log.e("connect()", "socket opening");
//			serialPort = manager.openSerialPort(serialPortName, param);
//			serialPort.registerEventListener(this);
//			serialPort.notifyOnDataAvailable(true);
//
//		} catch (TooManyListenersException e) {
//			e.printStackTrace();
//		} catch (PortInUseException e) {
//			e.printStackTrace();
//		} catch (NoSuchPortException e) {
//			e.printStackTrace();
//		} catch (UnsupportedCommOperationException e) {
//			e.printStackTrace();
//		}
//	}
//
//	private void send() {
//		try {
//			outputStream = serialPort.getOutputStream();
//
//			String cmdPacket = "Comm";
//
//			Log.e("send()", "Message sending");
//			outputStream.write(cmdPacket.getBytes());
//			outputStream.flush();
//			outputStream.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//
//	private void recv() {
//		try {
//			inputStream = serialPort.getInputStream();
//
//			int streamSize = inputStream.available();
//
//			if(streamSize > 0) {
//				byte[] buf = new byte[10];
//
//				Log.e("recv()", "Message receiving");
//				int recvMsgSize = inputStream.read(buf, 0, streamSize);
//
//				if(recvMsgSize <= 0) {
//					Log.e("recv()", "Message none");
//					return;
//				}
//
//				txt_recvMsg.setText(new String(buf));
//				Log.e("recv()", "Received message: " + new String(buf));
//
//			}
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//
//	private void close() {
//		STATUS_CONN = false;
//
//		if(serialPort != null) {
//			serialPort.close();
//			Log.e("close()", "socket close");
//		}
//
//		serialPort = null;
//	}
//	@Override
//	public void serialEvent(SerialPortEvent event) {
//		switch(event.getEventType()) {
//			case DATA_AVAILABLE:
//				try {
//					recv();
//				} catch (Exception e) {
//					close();
//				}
//				break;
//		}
//
//	}
//}
