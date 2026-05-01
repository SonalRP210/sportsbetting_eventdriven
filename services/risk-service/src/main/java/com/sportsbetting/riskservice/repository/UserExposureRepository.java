package com.sportsbetting.riskservice.repository;

import com.sportsbetting.riskservice.model.UserExposureEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserExposureRepository extends JpaRepository<UserExposureEntity, String> {
}
