package com.example.DCMapplication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/devices")
public class DeviceController {

    @Autowired
    private DeviceRepository deviceRepository;

    @GetMapping
    public List<Device> getAllDevices() {
        return deviceRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Device> getDeviceById(@PathVariable Long id) {
        return deviceRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Device createDevice(@RequestBody Device device) {
        return deviceRepository.save(device);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id) {
        if (deviceRepository.existsById(id)) {
            deviceRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/search")
    public List<Device> searchByField(
            @RequestParam String field,
            @RequestParam String value) {
        return switch (field.toLowerCase()) {
            case "name" -> deviceRepository.findByNameContainingIgnoreCase(value);
            case "owner" -> deviceRepository.findByOwner(value);
            case "macaddress" -> deviceRepository.findByMacAddress(value);
            case "type" -> deviceRepository.findByType(value);
            case "serialnumber" -> deviceRepository.findBySerialNumber(value);
            case "ipaddress" -> deviceRepository.findByIpAddress(value);
            case "config" -> deviceRepository.findByConfig(value);
            default -> throw new IllegalArgumentException("Invalid search field: " + field);
        };
    }

}