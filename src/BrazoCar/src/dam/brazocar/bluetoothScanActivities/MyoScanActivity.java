/*
 * BrazoCar - Desarrollo de Aplicaciones Móviles
 * Curso 2015-2016
 * Desarrollado por Javier Martínez y Samuel Veloso
 */

package dam.brazocar.bluetoothScanActivities;

import dam.brazocar.R;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import java.util.ArrayList;

/**
 * Activity to search for a myo device
 * @author Javier Martínez Arrieta
 * @author Samuel Veloso López
 */
public class MyoScanActivity extends Activity {
	
	public static final String MYO_MAC_ADDRESS = "MYO_MAC_ADDRESS";
	private static final String MYO_MANUFACTURER_MAC_PREFIX = "C2:85:B7";
	
	private BluetoothAdapter bluetooth_adapter;
	
	private ListView devices_list_view;
	private ArrayAdapter<String> devices_list_view_adapter;
	private ArrayList<BluetoothDevice> devices;
	
	private BroadcastReceiver myo_scanner;
	
	/**
	 * Method called when MainActivity is created
	 * @param savedInstancetate - Parameters and other elements, values, etc that where saved when the activity was destroyed
	 */
	protected void onCreate(Bundle savedInstanceState) {
		
		/* Bluetooth Myo devices layout is loaded */
		super.onCreate(savedInstanceState);		
		setContentView(R.layout.myo_scan_activity);
		
		/* Set title */
		final TextView title_view = (TextView) findViewById(R.id.title_id);
		title_view.setText(R.string.myo_devices_activity);
		title_view.setTypeface(Typeface.createFromAsset(getAssets(), "font/capsmall.ttf"));
		title_view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40);
		
		/* Init Bluetooth service */
		bluetooth_adapter = BluetoothAdapter.getDefaultAdapter();
		
		/* Assign an adapter to the ListView */
		devices_list_view = (ListView) findViewById(R.id.myo_list_devices);		
		devices_list_view_adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
		devices = new ArrayList<BluetoothDevice>();
		
		/* Notify the scan */
		Toast.makeText(getApplicationContext(), "Searching for Myo devices", Toast.LENGTH_SHORT).show();
		
		/* Start scanner */
		IntentFilter intent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		myo_scanner = new MyoDevicesScanner();
        registerReceiver(myo_scanner, intent);
        
        /* Refresh event */
		Button button_refresh = (Button) findViewById(R.id.button_myo_scan_refresh);
		button_refresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	unregisterReceiver(myo_scanner);
            	IntentFilter intent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        		myo_scanner = new MyoDevicesScanner();
                registerReceiver(myo_scanner, intent);
            }
        });
        
        /* Create event to manage device selection */
		devices_list_view.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

				/* Get selected device */
				BluetoothDevice device = (BluetoothDevice) devices.get(position);
				
				/* Return MAC address to parent activity */
				Intent intent = new Intent();
				intent.putExtra(MYO_MAC_ADDRESS, device.getAddress());
		        setResult(android.app.Activity.RESULT_OK, intent);
		        
		        /* Close activity */
		        finish();
			}
        });
	}

    private class MyoDevicesScanner extends BroadcastReceiver {
    	
    	/**
    	 * Main constructor
    	 */
    	public MyoDevicesScanner() {

    		/* Init adapters to save the data in */		
			devices.clear();
			devices_list_view_adapter.clear();
			devices_list_view.setAdapter(devices_list_view_adapter);
			
			/* Start devices scan */
			bluetooth_adapter.cancelDiscovery();
			bluetooth_adapter.startDiscovery();
		}
        
		/**
		 * Add found myo devices to the list shown to the user
		 * @param context - Context of the application
		 * @param intent- Intent that contains information of a found device
		 */
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            /* When discovery finds a device */
            if(BluetoothDevice.ACTION_FOUND.equals(action)) {
            	/* Get the BluetoothDevice object from the Intent */
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		    	/* Add the name and the address to an array adapter to show in a ListView */
                if(device.getAddress().startsWith(MYO_MANUFACTURER_MAC_PREFIX)) {
                	if(!devices.contains(device)) {
                		devices_list_view_adapter.add(device.getName() + "\n" + device.getAddress());
                		devices.add(device);
                	}
                }
            }
        }
    };
    
    /**
     * Method called when the activity is finishing
     */
	protected void onDestroy() {
        unregisterReceiver(myo_scanner);
        bluetooth_adapter.cancelDiscovery();
        super.onDestroy();
    }
	
}
