/*
 * Copyright 2024-2025 NetCracker Technology Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.qubership.itool.tasks.parsing.other;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.qubership.itool.utils.FSUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParseSearchPasswordInYamlVerticleTest {
    private static ParseSearchPasswordInYamlVerticle parser;
    private static List<String> positiveScenarios = List.of(
            "password: ${CERTIFICATE_FILE_PASSWORD:changeit}",
            "dba.password: ${ROOT_PASSWORD:pass}",
            "- MONGO_INITDB_ROOT_PASSWORD = ${MONGO_TEST_USER_PASSWORD:-test}",
            "password: ${POSTGRES_PASSWORD:postgres}",
            "password: p}assword",
            "password: p{assword",
            "password:'${password}'",
            "password :pa$$w0rd123",
            "password: \"password\"",
            "MINIO_ROOT_PASSWORD: password",
            "value: 'org.apache.kafka.common.security.scram.ScramLoginModule required username=\"{{ .Values.KAFKA_USERNAME }}\" password=\"pa$sword\";'",
            "saslJaasConfig: \"org.apache.kafka.common.security.scram.ScramLoginModule required username=client password=client;\"",
            "- POSTGRES_PASSWORD=password",
            "password: \"multiline\\nmysecret\\ntest\"",
            "ROOT_PASSWORD: rootpassword",
            "kafkaAuthPassword: \"admin\" # censored modification",
            "P_PASSWORD: Md!1[[]]\\\\{{jjkkl}}vRFvVSz##@I5G57Mm5$())))_x=Li=Dze=OBGd#Gt8VhtQMsds5v"
    );
    static List<String> stringArrayList = new ArrayList<>();

    @BeforeAll
    public static void setUp() throws IOException {
        stringArrayList = readData();
    }

    @Test
    public void testParseSingleFile_PositiveScenario() {
        List<String> result = filterByPattern();
        assertEquals(positiveScenarios, result);
    }

    private static List<String> filterByPattern() {
        List<String> result = new ArrayList<>();
        for (String singleElement : stringArrayList) {
            if (parser.checkPattern(singleElement)) {
                result.add(singleElement.trim());
            }
        }
        return result;
    }

    private static List<String> readData() throws IOException {
        // Create a parser instance
        parser = new ParseSearchPasswordInYamlVerticle();

        try (InputStream inputStream = FSUtils.openUrlStream(ParseSearchPasswordInYamlVerticleTest.class, "classpath:/parsing/other/possiblePasswords.txt")) {
            if (inputStream == null) {
                throw new IOException("Unable to open the URL stream");
            }
            InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);

            BufferedReader bufferedReader = new BufferedReader(streamReader);

            return bufferedReader.lines().collect(Collectors.toList());
        }
    }

}