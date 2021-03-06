/*
 *
 *  Copyright 2018 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package com.netflix.genie.agent.execution.statemachine.actions

import com.netflix.genie.agent.execution.ExecutionContext
import com.netflix.genie.agent.execution.statemachine.Events
import com.netflix.genie.common.internal.dto.v4.JobSpecification
import com.netflix.genie.common.internal.exceptions.JobArchiveException
import com.netflix.genie.common.internal.services.JobArchiveService
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class ShutdownActionSpec extends Specification {
    @Rule
    TemporaryFolder temporaryFolder
    ExecutionContext executionContext
    JobArchiveService archivalService
    ShutdownAction action
    JobSpecification jobSpecification
    File jobDir
    URI sampleS3URI = new URI("s3://test-bucket/genie/job/test123")

    void setup() {
        this.executionContext = Mock(ExecutionContext)
        this.archivalService = Mock(JobArchiveService)
        this.jobSpecification = Mock(JobSpecification)
        this.jobDir = temporaryFolder.newFolder("job")
        this.action = new ShutdownAction(executionContext, archivalService)
    }

    void cleanup() {
    }

    def "Archive if archival location is provided"() {
        def event

        when:
        event = action.executeStateAction(executionContext)

        then:
        2 * executionContext.getJobSpecification() >> Optional.of(jobSpecification)
        2 * jobSpecification.getArchiveLocation() >> Optional.of(sampleS3URI.toString())
        2 * executionContext.getJobDirectory() >> Optional.of(jobDir)
        1 * archivalService.archiveDirectory(jobDir.toPath(), sampleS3URI)
        event == Events.SHUTDOWN_COMPLETE

    }

    def "Don't archive if archival location is not provided"() {
        def event

        when:
        event = action.executeStateAction(executionContext)

        then:
        2 * executionContext.getJobSpecification() >> Optional.of(jobSpecification)
        1 * jobSpecification.getArchiveLocation() >> Optional.empty()
        0 * executionContext.getJobDirectory() >> Optional.of(jobDir)
        0 * archivalService.archiveDirectory(_, _)
        event == Events.SHUTDOWN_COMPLETE
    }

    def "Don't archive if archival location is not empty"() {
        def event
        when:
        event = action.executeStateAction(executionContext)

        then: "Skip archival for a empty archiveDirectory location"
        2 * executionContext.getJobSpecification() >> Optional.of(jobSpecification)
        2 * jobSpecification.getArchiveLocation() >> Optional.of("")
        0 * executionContext.getJobDirectory() >> Optional.of(jobDir)
        0 * archivalService.archiveDirectory(_, _)
        event == Events.SHUTDOWN_COMPLETE
    }

    def "Swallow archival exception"() {
        def event

        when:
        event = action.executeStateAction(executionContext)

        then:
        2 * executionContext.getJobSpecification() >> Optional.of(jobSpecification)
        2 * jobSpecification.getArchiveLocation() >> Optional.of(sampleS3URI.toString())
        2 * executionContext.getJobDirectory() >> Optional.of(jobDir)
        1 * archivalService.archiveDirectory(jobDir.toPath(), sampleS3URI) >> {
            throw new JobArchiveException("error")
        }
        event == Events.SHUTDOWN_COMPLETE
    }

    def "Swallow bad archival URI exception"() {
        def event

        when:
        event = action.executeStateAction(executionContext)

        then:
        2 * executionContext.getJobSpecification() >> Optional.of(jobSpecification)
        2 * jobSpecification.getArchiveLocation() >> Optional.of("invalid URI")
        2 * executionContext.getJobDirectory() >> Optional.of(jobDir)
        0 * archivalService.archiveDirectory(jobDir.toPath(), sampleS3URI)
        event == Events.SHUTDOWN_COMPLETE
    }


    def "Pre and post action validation"() {
        when:
        action.executePreActionValidation()

        then:
        noExceptionThrown()

        when:
        action.executePostActionValidation()

        then:
        noExceptionThrown()
    }
}
