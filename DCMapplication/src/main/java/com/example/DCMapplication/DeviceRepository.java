package com.example.DCMapplication;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    List<Device> findByNameContainingIgnoreCase(String name);
    List<Device> findByOwner(String owner);
    List<Device> findByMacAddress(String macAddress);
    List<Device> findByType(String type);
    List<Device> findBySerialNumber(String serialNumber);
    List<Device> findByIpAddress(String ipAddress);
    List<Device> findByConfig(String config);

}