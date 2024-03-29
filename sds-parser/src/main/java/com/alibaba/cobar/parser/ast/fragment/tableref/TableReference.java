/*
 * Copyright 1999-2012 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * (created at 2011-5-10)
 */
package com.alibaba.cobar.parser.ast.fragment.tableref;

import com.alibaba.cobar.parser.ast.ASTNode;

/**
 * @author <a href="mailto:shuo.qius@alibaba-inc.com">QIU Shuo</a>
 */
public interface TableReference extends ASTNode {

    int PRECEDENCE_REFS = 0;
    int PRECEDENCE_JOIN = 1;
    int PRECEDENCE_FACTOR = 2;

    /**
     * remove last condition element is success
     *
     * @return {@link java.util.List List&lt;String&gt;} or {@link com.alibaba.cobar.parser.ast.expression.Expression
     * Expression}. null if last condition element cannot be removed.
     */
    Object removeLastConditionElement();

    /**
     * @return true if and only if there is one table (not subquery) in table reference
     */
    public boolean isSingleTable();

    /**
     * @return precedences are defined in {@link com.alibaba.cobar.parser.ast.fragment.tableref.TableReference}
     */
    int getPrecedence();
}
