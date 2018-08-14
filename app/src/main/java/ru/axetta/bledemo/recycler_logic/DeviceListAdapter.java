package ru.axetta.bledemo.recycler_logic;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

import ru.axetta.bledemo.R;
import ru.axetta.bledemo.ble.BluetoothLe;

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.DeviceViewHolder> {

    private List<BluetoothLe.DeviceLe> deviceList;

    public DeviceListAdapter(List<BluetoothLe.DeviceLe> deviceList) {
        this.deviceList = deviceList;
    }

    @NonNull
    @Override
    public DeviceListAdapter.DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_view_holder, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceListAdapter.DeviceViewHolder holder, int position) {
        BluetoothLe.DeviceLe device = deviceList.get(position);
        holder.name.setText(device.getDevice().getName());
        holder.address.setText(device.getDevice().getAddress());
        holder.rssi.setText(String.format(Locale.getDefault(),"%d dBm", device.getRssi()));
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView name, address, rssi;
        public DeviceViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.deviceName);
            address = itemView.findViewById(R.id.deviceAddress);
            rssi = itemView.findViewById(R.id.deviceRssi);
        }
    }
}
