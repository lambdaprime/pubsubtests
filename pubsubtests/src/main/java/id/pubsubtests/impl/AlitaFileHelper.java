/*
 * Copyright 2023 pubsubtests project
 * 
 * Website: https://github.com/lambdaprime/pubsubtests
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package id.pubsubtests.impl;

import id.xfunction.ResourceUtils;
import id.xfunction.XUtils;
import id.xfunction.nio.file.XFiles;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author lambdaprime intid@protonmail.com
 */
public class AlitaFileHelper {

    public static final Integer SIZE_IN_BYTES = 6_111_377;
    private static final String RESOURCE_NAME = "alita";

    public static Path extractToTempFolderIfMissing() {
        var file = XFiles.TEMP_FOLDER.orElseThrow().resolve(RESOURCE_NAME);
        if (!Files.exists(file)) {
            new ResourceUtils().extractResource(RESOURCE_NAME, file);
        }
        return file;
    }

    public static boolean isEquals(byte[] item) {
        try {
            return "e777fdaf42a3f8a0567edccb3ad3ee2e".equals(XUtils.md5Sum(item));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
