

# AgencyAdminResponseDTO


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**id** | **Long** |  |  [optional] |
|**name** | **String** |  |  [optional] |
|**description** | **String** |  |  [optional] |
|**postcode** | **String** |  |  [optional] |
|**city** | **String** |  |  [optional] |
|**offline** | **Boolean** |  |  [optional] |
|**consultingType** | **Integer** |  |  [optional] |
|**url** | **String** |  |  [optional] |
|**external** | **Boolean** |  |  [optional] |
|**topics** | [**List&lt;TopicDTO&gt;**](TopicDTO.md) |  |  [optional] |
|**demographics** | [**DemographicsDTO**](DemographicsDTO.md) |  |  [optional] |
|**createDate** | **String** |  |  [optional] |
|**updateDate** | **String** |  |  [optional] |
|**deleteDate** | **String** |  |  [optional] |
|**counsellingRelations** | [**List&lt;CounsellingRelationsEnum&gt;**](#List&lt;CounsellingRelationsEnum&gt;) |  |  [optional] |
|**tenantId** | **Long** |  |  [optional] |
|**dataProtection** | [**DataProtectionDTO**](DataProtectionDTO.md) |  |  [optional] |
|**agencyLogo** | **String** |  |  [optional] |



## Enum: List&lt;CounsellingRelationsEnum&gt;

| Name | Value |
|---- | -----|
| RELATIVE_COUNSELLING | &quot;RELATIVE_COUNSELLING&quot; |
| SELF_COUNSELLING | &quot;SELF_COUNSELLING&quot; |
| PARENTAL_COUNSELLING | &quot;PARENTAL_COUNSELLING&quot; |



