package ru.axetta.bledemo.recycler_logic;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;


import ru.axetta.bledemo.MainActivity;
import ru.axetta.bledemo.R;
import ru.axetta.bledemo.ble.BluetoothLe;

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.DeviceViewHolder> {

    private List<BluetoothLe.DeviceLe> deviceList;

    private Context context;

    public DeviceListAdapter(List<BluetoothLe.DeviceLe> deviceList, Context context) {
        this.deviceList = deviceList;
        this.context = context;
    }

    @NonNull
    @Override
    public DeviceListAdapter.DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_view_holder, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void onBindViewHolder(@NonNull DeviceListAdapter.DeviceViewHolder holder, int position) {
        final BluetoothLe.DeviceLe device = deviceList.get(position);
        holder.name.setText(device.getDevice().getName());
        holder.address.setText(device.getDevice().getAddress());
        holder.rssi.setText(String.format(Locale.getDefault(),"%d dBm", device.getRssi()));
        holder.container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity)context).changeUi(device);
//                Intent deviceActivity = new Intent(context, DeviceActivity.class);
//                deviceActivity.putExtra(Consts.DIVECE_NAME, device.getDevice().getName());
//                deviceActivity.putExtra(Consts.DEVICE, device.getDevice());
//                context.startActivity(deviceActivity);
            }
        });
}

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView name, address, rssi;
        ConstraintLayout container;
        DeviceViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.deviceName);
            address = itemView.findViewById(R.id.deviceAddress);
            rssi = itemView.findViewById(R.id.deviceRssi);
            container = itemView.findViewById(R.id.container);
        }
        void setOnDeviceClickListener(BluetoothLe.DeviceLe deviceClickListener, final OnDeviceClickListener listener, final int position) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onDeviceClick(deviceList.get(position));
                }
            });
        }
    }

}
