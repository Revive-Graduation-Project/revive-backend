package com.restaurant.menu.service;

import com.restaurant.menu.dto.BuildOptionsResponse;

public interface CustomizationService {
    BuildOptionsResponse getBuildOptions(String primaryCategory);
}
