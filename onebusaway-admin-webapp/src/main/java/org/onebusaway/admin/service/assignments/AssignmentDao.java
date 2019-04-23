/**
 * Copyright (C) 2019 Cambridge Systematics, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.admin.service.assignments;

import org.onebusaway.admin.model.assignments.Assignment;

import java.util.List;

public interface AssignmentDao {
    public List<Assignment> getAll();
    public Assignment getAssignment(String blockId);
    public void save(Assignment assignment);
    public void saveAll(List<Assignment> assignmentList);
    public void delete(Assignment assignment);
    public void deleteAll();
}
