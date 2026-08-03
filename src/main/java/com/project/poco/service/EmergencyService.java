package com.project.poco.service;

import com.project.poco.entity.EmergencyDispatch;
import com.project.poco.entity.ResponseStatus;
import com.project.poco.repository.EmergencyDispatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmergencyService {

    private final EmergencyDispatchRepository emergencyDispatchRepository;

    public EmergencyDispatch dispatch(Long deviceId, Long dispatchedBy) {
        EmergencyDispatch dispatch = new EmergencyDispatch();
        dispatch.setDeviceId(deviceId);
        dispatch.setDispatchedBy(dispatchedBy);
        dispatch.setResponseStatus(ResponseStatus.REQUESTED);
        return emergencyDispatchRepository.save(dispatch);
    }

    public List<EmergencyDispatch> findByDeviceId(Long deviceId) {
        return emergencyDispatchRepository.findByDeviceId(deviceId);
    }
}
