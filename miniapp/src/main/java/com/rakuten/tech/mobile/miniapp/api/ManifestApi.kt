package com.rakuten.tech.mobile.miniapp.api

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

internal interface ManifestApi {
    // double slash is okay
    @GET("host/{hostId}/miniapp/{miniappId}/version/{versionId}/{testPath}/manifest")
    fun fetchFileListFromManifest(
        @Path("hostId") hostId: String,
        @Path("miniappId") miniAppId: String,
        @Path("versionId") versionId: String,
        @Path("testPath") testPath: String = ""
    ): Call<ManifestEntity>
}

@Keep
internal data class ManifestEntity(
    @SerializedName("manifest") val files: List<String>,
    val publicKeyId: String
)

@Keep
internal data class ManifestHeader(
    val signature: String?
)
