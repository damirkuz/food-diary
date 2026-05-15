<#import "/spring.ftl" as spring>

<#macro field path>
    <@spring.bind path />

    <#if spring.status.errorMessages?has_content>
        <div class="invalid-feedback d-block">
            <#list spring.status.errorMessages as error>
                <div>${error}</div>
            </#list>
        </div>
    </#if>
</#macro>
