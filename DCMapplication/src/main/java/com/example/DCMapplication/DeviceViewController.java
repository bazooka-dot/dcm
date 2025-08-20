package com.example.DCMapplication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class DeviceViewController {

    @Autowired
    private DeviceRepository deviceRepository;

    @GetMapping("/devices/view")
    public String viewDevices(Model model) {
        model.addAttribute("devices", deviceRepository.findAll());
        return "devices";
    }

    @PostMapping("/devices/view/add")
    public String addDevice(Device device) {
        deviceRepository.save(device);
        return "redirect:/devices/view";
    }

    @GetMapping("/devices/view/search")
    public String searchDevices(@RequestParam String field, @RequestParam String value, Model model) {
        // Simple switch for search, matching DeviceController logic
        switch (field.toLowerCase()) {
            case "name" -> model.addAttribute("devices", deviceRepository.findByNameContainingIgnoreCase(value));
            case "owner" -> model.addAttribute("devices", deviceRepository.findByOwner(value));
            case "macaddress" -> model.addAttribute("devices", deviceRepository.findByMacAddress(value));
            case "type" -> model.addAttribute("devices", deviceRepository.findByType(value));
            case "serialnumber" -> model.addAttribute("devices", deviceRepository.findBySerialNumber(value));
            case "ipaddress" -> model.addAttribute("devices", deviceRepository.findByIpAddress(value));
            case "config" -> model.addAttribute("devices", deviceRepository.findByConfig(value));
            default -> model.addAttribute("devices", deviceRepository.findAll());
        }
        return "devices";
    }

    @PostMapping("/devices/view/delete/{id}")
    public String deleteDevice(@PathVariable Long id) {
        deviceRepository.deleteById(id);
        return "redirect:/devices/view";
    }
}