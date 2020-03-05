/*
 *
 * Copyright 2018 Odysseus Data Services, inc.
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
 *
 * Company: Odysseus Data Services, Inc.
 * Product Owner/Architecture: Gregory Klebanov
 * Authors: Pavel Grafkin, Alexandr Ryabokon, Vitaly Koulakov, Anton Gackovka, Maria Pozhidaeva, Mikhail Mironov
 * Created: May 17, 2017
 *
 */

package com.odysseusinc.arachne.executionengine.util;

import java.time.Duration;
import java.util.Date;
import org.apache.commons.lang3.time.DurationFormatUtils;

public class DateUtil {

    public static String defaultFormat(String format, Date date) {

        return date != null ? String.format(format, date) : "";
    }

    public static String formatDuration(Duration duration) {

        return duration.toMillis() < 1000 ? String.format("%d ms", duration.toMillis()) :
                DurationFormatUtils.formatDurationWords(duration.toMillis(), true, true);
    }
}
