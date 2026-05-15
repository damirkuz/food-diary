<#macro flash>
    <#if successMessage??>
        <div class="alert alert-success border-0 shadow-sm">${successMessage}</div>
    </#if>

    <#if errorMessage??>
        <div class="alert alert-danger border-0 shadow-sm">${errorMessage}</div>
    </#if>
</#macro>
