/*
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
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
package com.exadel.etoolbox.anydiff;

import com.exadel.etoolbox.anydiff.comparison.DiffBlockXPathTest;
import com.exadel.etoolbox.anydiff.comparison.DiffCountTest;
import com.exadel.etoolbox.anydiff.comparison.DiffTaskTest;
import com.exadel.etoolbox.anydiff.comparison.DiffTest;
import com.exadel.etoolbox.anydiff.comparison.FragmentTest;
import com.exadel.etoolbox.anydiff.comparison.MarkedStringTest;
import com.exadel.etoolbox.anydiff.comparison.SpacesHandlingTest;
import com.exadel.etoolbox.anydiff.comparison.preprocessor.PreprocessorsTest;
import com.exadel.etoolbox.anydiff.runner.DiffRunnerTest;
import com.exadel.etoolbox.anydiff.runner.FilterHelperTest;
import com.exadel.etoolbox.anydiff.runner.FiltersTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        DiffTest.class,
        DiffRunnerTest.class,
        DiffTaskTest.class,

        DiffCountTest.class,
        DiffBlockXPathTest.class,

        FragmentTest.class,
        MarkedStringTest.class,

        PreprocessorsTest.class,
        SpacesHandlingTest.class,

        FilterHelperTest.class,
        FiltersTest.class
})
public class AllTests {
}
