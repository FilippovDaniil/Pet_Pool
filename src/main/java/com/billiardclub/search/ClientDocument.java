package com.billiardclub.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientDocument {

    private String id;       // _id в OpenSearch — всегда String (даже если в БД Long)
    private String fullName;
    private String rank;
    private String phone;
}
