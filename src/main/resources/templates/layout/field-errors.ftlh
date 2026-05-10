<#import "/spring.ftl" as spring>

<#macro field path>
    <@spring.bind path />

    <#if spring.status.errorMessages?has_content>
        <div class="field-errors">
            <#list spring.status.errorMessages as error>
                <div class="field-error">${error}</div>
            </#list>
        </div>
    </#if>
</#macro>