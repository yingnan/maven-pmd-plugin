/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugins.pmd;

import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.RuleSetLoader;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class RuleSetTest {

    private RuleSetLoader ruleSetLoader =
            RuleSetLoader.fromPmdConfig(new PMDConfiguration()).warnDeprecated(true);

    @Before
    public void setUp() {
        CapturingPrintStream.init(true);
    }

    @Test
    public void testDefaultRuleset() {
        ruleSetLoader.loadFromResource("rulesets/java/maven-pmd-plugin-default.xml");
        assertNoDeprecatedRules(CapturingPrintStream.getOutput());
    }

    @Test
    public void testMavenRuleset() {
        ruleSetLoader.loadFromResource("rulesets/maven.xml");
        assertNoDeprecatedRules(CapturingPrintStream.getOutput());
    }

    private void assertNoDeprecatedRules(String output) {
        // there must be no warnings (like deprecated rules) in the log output
        assertFalse(output.contains("deprecated Rule name"));
        assertFalse(output.contains("Discontinue using Rule name"));
        assertFalse(output.contains("is referenced multiple times"));
    }
}
