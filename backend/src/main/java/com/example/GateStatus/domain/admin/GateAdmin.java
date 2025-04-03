package com.example.GateStatus.domain.admin;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class GateAdmin {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private Long id;

    private Role role;

}
