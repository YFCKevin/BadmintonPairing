package com.yfckevin.badmintonPairing.entity;

import jakarta.persistence.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "member")
public class Member {
    @Id
    private String id;
}
