package com.vi.tenantservice.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * TenantAdminAllowedPermissionToggles
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-03-11T15:41:30.641955926Z[Etc/UTC]")
public class TenantAdminAllowedPermissionToggles {

  private Boolean anonymousChat;

  private Boolean calls;

  private Boolean supervision;

  private Boolean supervisionAnonymousChats;

  private Boolean supervisionOneOnOneChats;

  private Boolean audioCalls;

  private Boolean audioCallsAnonymousChats;

  private Boolean audioCallsOneOnOneChats;

  private Boolean audioCallsGroupChats;

  private Boolean audioCallsSupervisionChats;

  private Boolean videoCalls;

  private Boolean videoCallsAnonymousChats;

  private Boolean videoCallsOneOnOneChats;

  private Boolean videoCallsGroupChats;

  private Boolean videoCallsSupervisionChats;

  private Boolean threads;

  private Boolean threadsAnonymousChats;

  private Boolean threadsOneOnOneChats;

  private Boolean threadsGroupChats;

  private Boolean threadsSupervisionChats;

  private Boolean voiceMessages;

  private Boolean voiceMessagesAnonymousChats;

  private Boolean voiceMessagesOneOnOneChats;

  private Boolean voiceMessagesGroupChats;

  private Boolean voiceMessagesSupervisionChats;

  public TenantAdminAllowedPermissionToggles anonymousChat(Boolean anonymousChat) {
    this.anonymousChat = anonymousChat;
    return this;
  }

  /**
   * Get anonymousChat
   * @return anonymousChat
  */
  
  @Schema(name = "anonymousChat", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("anonymousChat")
  public Boolean getAnonymousChat() {
    return anonymousChat;
  }

  public void setAnonymousChat(Boolean anonymousChat) {
    this.anonymousChat = anonymousChat;
  }

  public TenantAdminAllowedPermissionToggles calls(Boolean calls) {
    this.calls = calls;
    return this;
  }

  /**
   * Get calls
   * @return calls
  */
  
  @Schema(name = "calls", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("calls")
  public Boolean getCalls() {
    return calls;
  }

  public void setCalls(Boolean calls) {
    this.calls = calls;
  }

  public TenantAdminAllowedPermissionToggles supervision(Boolean supervision) {
    this.supervision = supervision;
    return this;
  }

  /**
   * Get supervision
   * @return supervision
  */
  
  @Schema(name = "supervision", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("supervision")
  public Boolean getSupervision() {
    return supervision;
  }

  public void setSupervision(Boolean supervision) {
    this.supervision = supervision;
  }

  public TenantAdminAllowedPermissionToggles supervisionAnonymousChats(Boolean supervisionAnonymousChats) {
    this.supervisionAnonymousChats = supervisionAnonymousChats;
    return this;
  }

  /**
   * Get supervisionAnonymousChats
   * @return supervisionAnonymousChats
  */
  
  @Schema(name = "supervisionAnonymousChats", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("supervisionAnonymousChats")
  public Boolean getSupervisionAnonymousChats() {
    return supervisionAnonymousChats;
  }

  public void setSupervisionAnonymousChats(Boolean supervisionAnonymousChats) {
    this.supervisionAnonymousChats = supervisionAnonymousChats;
  }

  public TenantAdminAllowedPermissionToggles supervisionOneOnOneChats(Boolean supervisionOneOnOneChats) {
    this.supervisionOneOnOneChats = supervisionOneOnOneChats;
    return this;
  }

  /**
   * Get supervisionOneOnOneChats
   * @return supervisionOneOnOneChats
  */
  
  @Schema(name = "supervisionOneOnOneChats", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("supervisionOneOnOneChats")
  public Boolean getSupervisionOneOnOneChats() {
    return supervisionOneOnOneChats;
  }

  public void setSupervisionOneOnOneChats(Boolean supervisionOneOnOneChats) {
    this.supervisionOneOnOneChats = supervisionOneOnOneChats;
  }

  public TenantAdminAllowedPermissionToggles audioCalls(Boolean audioCalls) {
    this.audioCalls = audioCalls;
    return this;
  }

  /**
   * Get audioCalls
   * @return audioCalls
  */
  
  @Schema(name = "audioCalls", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("audioCalls")
  public Boolean getAudioCalls() {
    return audioCalls;
  }

  public void setAudioCalls(Boolean audioCalls) {
    this.audioCalls = audioCalls;
  }

  public TenantAdminAllowedPermissionToggles audioCallsAnonymousChats(Boolean audioCallsAnonymousChats) {
    this.audioCallsAnonymousChats = audioCallsAnonymousChats;
    return this;
  }

  /**
   * Get audioCallsAnonymousChats
   * @return audioCallsAnonymousChats
  */
  
  @Schema(name = "audioCallsAnonymousChats", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("audioCallsAnonymousChats")
  public Boolean getAudioCallsAnonymousChats() {
    return audioCallsAnonymousChats;
  }

  public void setAudioCallsAnonymousChats(Boolean audioCallsAnonymousChats) {
    this.audioCallsAnonymousChats = audioCallsAnonymousChats;
  }

  public TenantAdminAllowedPermissionToggles audioCallsOneOnOneChats(Boolean audioCallsOneOnOneChats) {
    this.audioCallsOneOnOneChats = audioCallsOneOnOneChats;
    return this;
  }

  /**
   * Get audioCallsOneOnOneChats
   * @return audioCallsOneOnOneChats
  */
  
  @Schema(name = "audioCallsOneOnOneChats", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("audioCallsOneOnOneChats")
  public Boolean getAudioCallsOneOnOneChats() {
    return audioCallsOneOnOneChats;
  }

  public void setAudioCallsOneOnOneChats(Boolean audioCallsOneOnOneChats) {
    this.audioCallsOneOnOneChats = audioCallsOneOnOneChats;
  }

  public TenantAdminAllowedPermissionToggles audioCallsGroupChats(Boolean audioCallsGroupChats) {
    this.audioCallsGroupChats = audioCallsGroupChats;
    return this;
  }

  /**
   * Get audioCallsGroupChats
   * @return audioCallsGroupChats
  */
  
  @Schema(name = "audioCallsGroupChats", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("audioCallsGroupChats")
  public Boolean getAudioCallsGroupChats() {
    return audioCallsGroupChats;
  }

  public void setAudioCallsGroupChats(Boolean audioCallsGroupChats) {
    this.audioCallsGroupChats = audioCallsGroupChats;
  }

  public TenantAdminAllowedPermissionToggles audioCallsSupervisionChats(Boolean audioCallsSupervisionChats) {
    this.audioCallsSupervisionChats = audioCallsSupervisionChats;
    return this;
  }

  /**
   * Get audioCallsSupervisionChats
   * @return audioCallsSupervisionChats
  */
  
  @Schema(name = "audioCallsSupervisionChats", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("audioCallsSupervisionChats")
  public Boolean getAudioCallsSupervisionChats() {
    return audioCallsSupervisionChats;
  }

  public void setAudioCallsSupervisionChats(Boolean audioCallsSupervisionChats) {
    this.audioCallsSupervisionChats = audioCallsSupervisionChats;
  }

  public TenantAdminAllowedPermissionToggles videoCalls(Boolean videoCalls) {
    this.videoCalls = videoCalls;
    return this;
  }

  /**
   * Get videoCalls
   * @return videoCalls
  */
  
  @Schema(name = "videoCalls", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("videoCalls")
  public Boolean getVideoCalls() {
    return videoCalls;
  }

  public void setVideoCalls(Boolean videoCalls) {
    this.videoCalls = videoCalls;
  }

  public TenantAdminAllowedPermissionToggles videoCallsAnonymousChats(Boolean videoCallsAnonymousChats) {
    this.videoCallsAnonymousChats = videoCallsAnonymousChats;
    return this;
  }

  /**
   * Get videoCallsAnonymousChats
   * @return videoCallsAnonymousChats
  */
  
  @Schema(name = "videoCallsAnonymousChats", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("videoCallsAnonymousChats")
  public Boolean getVideoCallsAnonymousChats() {
    return videoCallsAnonymousChats;
  }

  public void setVideoCallsAnonymousChats(Boolean videoCallsAnonymousChats) {
    this.videoCallsAnonymousChats = videoCallsAnonymousChats;
  }

  public TenantAdminAllowedPermissionToggles videoCallsOneOnOneChats(Boolean videoCallsOneOnOneChats) {
    this.videoCallsOneOnOneChats = videoCallsOneOnOneChats;
    return this;
  }

  /**
   * Get videoCallsOneOnOneChats
   * @return videoCallsOneOnOneChats
  */
  
  @Schema(name = "videoCallsOneOnOneChats", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("videoCallsOneOnOneChats")
  public Boolean getVideoCallsOneOnOneChats() {
    return videoCallsOneOnOneChats;
  }

  public void setVideoCallsOneOnOneChats(Boolean videoCallsOneOnOneChats) {
    this.videoCallsOneOnOneChats = videoCallsOneOnOneChats;
  }

  public TenantAdminAllowedPermissionToggles videoCallsGroupChats(Boolean videoCallsGroupChats) {
    this.videoCallsGroupChats = videoCallsGroupChats;
    return this;
  }

  /**
   * Get videoCallsGroupChats
   * @return videoCallsGroupChats
  */
  
  @Schema(name = "videoCallsGroupChats", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("videoCallsGroupChats")
  public Boolean getVideoCallsGroupChats() {
    return videoCallsGroupChats;
  }

  public void setVideoCallsGroupChats(Boolean videoCallsGroupChats) {
    this.videoCallsGroupChats = videoCallsGroupChats;
  }

  public TenantAdminAllowedPermissionToggles videoCallsSupervisionChats(Boolean videoCallsSupervisionChats) {
    this.videoCallsSupervisionChats = videoCallsSupervisionChats;
    return this;
  }

  /**
   * Get videoCallsSupervisionChats
   * @return videoCallsSupervisionChats
  */
  
  @Schema(name = "videoCallsSupervisionChats", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("videoCallsSupervisionChats")
  public Boolean getVideoCallsSupervisionChats() {
    return videoCallsSupervisionChats;
  }

  public void setVideoCallsSupervisionChats(Boolean videoCallsSupervisionChats) {
    this.videoCallsSupervisionChats = videoCallsSupervisionChats;
  }

  public TenantAdminAllowedPermissionToggles threads(Boolean threads) {
    this.threads = threads;
    return this;
  }

  /**
   * Get threads
   * @return threads
  */
  
  @Schema(name = "threads", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("threads")
  public Boolean getThreads() {
    return threads;
  }

  public void setThreads(Boolean threads) {
    this.threads = threads;
  }

  public TenantAdminAllowedPermissionToggles threadsAnonymousChats(Boolean threadsAnonymousChats) {
    this.threadsAnonymousChats = threadsAnonymousChats;
    return this;
  }

  /**
   * Get threadsAnonymousChats
   * @return threadsAnonymousChats
  */
  
  @Schema(name = "threadsAnonymousChats", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("threadsAnonymousChats")
  public Boolean getThreadsAnonymousChats() {
    return threadsAnonymousChats;
  }

  public void setThreadsAnonymousChats(Boolean threadsAnonymousChats) {
    this.threadsAnonymousChats = threadsAnonymousChats;
  }

  public TenantAdminAllowedPermissionToggles threadsOneOnOneChats(Boolean threadsOneOnOneChats) {
    this.threadsOneOnOneChats = threadsOneOnOneChats;
    return this;
  }

  /**
   * Get threadsOneOnOneChats
   * @return threadsOneOnOneChats
  */
  
  @Schema(name = "threadsOneOnOneChats", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("threadsOneOnOneChats")
  public Boolean getThreadsOneOnOneChats() {
    return threadsOneOnOneChats;
  }

  public void setThreadsOneOnOneChats(Boolean threadsOneOnOneChats) {
    this.threadsOneOnOneChats = threadsOneOnOneChats;
  }

  public TenantAdminAllowedPermissionToggles threadsGroupChats(Boolean threadsGroupChats) {
    this.threadsGroupChats = threadsGroupChats;
    return this;
  }

  /**
   * Get threadsGroupChats
   * @return threadsGroupChats
  */
  
  @Schema(name = "threadsGroupChats", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("threadsGroupChats")
  public Boolean getThreadsGroupChats() {
    return threadsGroupChats;
  }

  public void setThreadsGroupChats(Boolean threadsGroupChats) {
    this.threadsGroupChats = threadsGroupChats;
  }

  public TenantAdminAllowedPermissionToggles threadsSupervisionChats(Boolean threadsSupervisionChats) {
    this.threadsSupervisionChats = threadsSupervisionChats;
    return this;
  }

  /**
   * Get threadsSupervisionChats
   * @return threadsSupervisionChats
  */
  
  @Schema(name = "threadsSupervisionChats", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("threadsSupervisionChats")
  public Boolean getThreadsSupervisionChats() {
    return threadsSupervisionChats;
  }

  public void setThreadsSupervisionChats(Boolean threadsSupervisionChats) {
    this.threadsSupervisionChats = threadsSupervisionChats;
  }

  public TenantAdminAllowedPermissionToggles voiceMessages(Boolean voiceMessages) {
    this.voiceMessages = voiceMessages;
    return this;
  }

  /**
   * Get voiceMessages
   * @return voiceMessages
  */
  
  @Schema(name = "voiceMessages", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("voiceMessages")
  public Boolean getVoiceMessages() {
    return voiceMessages;
  }

  public void setVoiceMessages(Boolean voiceMessages) {
    this.voiceMessages = voiceMessages;
  }

  public TenantAdminAllowedPermissionToggles voiceMessagesAnonymousChats(Boolean voiceMessagesAnonymousChats) {
    this.voiceMessagesAnonymousChats = voiceMessagesAnonymousChats;
    return this;
  }

  /**
   * Get voiceMessagesAnonymousChats
   * @return voiceMessagesAnonymousChats
  */
  
  @Schema(name = "voiceMessagesAnonymousChats", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("voiceMessagesAnonymousChats")
  public Boolean getVoiceMessagesAnonymousChats() {
    return voiceMessagesAnonymousChats;
  }

  public void setVoiceMessagesAnonymousChats(Boolean voiceMessagesAnonymousChats) {
    this.voiceMessagesAnonymousChats = voiceMessagesAnonymousChats;
  }

  public TenantAdminAllowedPermissionToggles voiceMessagesOneOnOneChats(Boolean voiceMessagesOneOnOneChats) {
    this.voiceMessagesOneOnOneChats = voiceMessagesOneOnOneChats;
    return this;
  }

  /**
   * Get voiceMessagesOneOnOneChats
   * @return voiceMessagesOneOnOneChats
  */
  
  @Schema(name = "voiceMessagesOneOnOneChats", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("voiceMessagesOneOnOneChats")
  public Boolean getVoiceMessagesOneOnOneChats() {
    return voiceMessagesOneOnOneChats;
  }

  public void setVoiceMessagesOneOnOneChats(Boolean voiceMessagesOneOnOneChats) {
    this.voiceMessagesOneOnOneChats = voiceMessagesOneOnOneChats;
  }

  public TenantAdminAllowedPermissionToggles voiceMessagesGroupChats(Boolean voiceMessagesGroupChats) {
    this.voiceMessagesGroupChats = voiceMessagesGroupChats;
    return this;
  }

  /**
   * Get voiceMessagesGroupChats
   * @return voiceMessagesGroupChats
  */
  
  @Schema(name = "voiceMessagesGroupChats", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("voiceMessagesGroupChats")
  public Boolean getVoiceMessagesGroupChats() {
    return voiceMessagesGroupChats;
  }

  public void setVoiceMessagesGroupChats(Boolean voiceMessagesGroupChats) {
    this.voiceMessagesGroupChats = voiceMessagesGroupChats;
  }

  public TenantAdminAllowedPermissionToggles voiceMessagesSupervisionChats(Boolean voiceMessagesSupervisionChats) {
    this.voiceMessagesSupervisionChats = voiceMessagesSupervisionChats;
    return this;
  }

  /**
   * Get voiceMessagesSupervisionChats
   * @return voiceMessagesSupervisionChats
  */
  
  @Schema(name = "voiceMessagesSupervisionChats", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("voiceMessagesSupervisionChats")
  public Boolean getVoiceMessagesSupervisionChats() {
    return voiceMessagesSupervisionChats;
  }

  public void setVoiceMessagesSupervisionChats(Boolean voiceMessagesSupervisionChats) {
    this.voiceMessagesSupervisionChats = voiceMessagesSupervisionChats;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TenantAdminAllowedPermissionToggles tenantAdminAllowedPermissionToggles = (TenantAdminAllowedPermissionToggles) o;
    return Objects.equals(this.anonymousChat, tenantAdminAllowedPermissionToggles.anonymousChat) &&
        Objects.equals(this.calls, tenantAdminAllowedPermissionToggles.calls) &&
        Objects.equals(this.supervision, tenantAdminAllowedPermissionToggles.supervision) &&
        Objects.equals(this.supervisionAnonymousChats, tenantAdminAllowedPermissionToggles.supervisionAnonymousChats) &&
        Objects.equals(this.supervisionOneOnOneChats, tenantAdminAllowedPermissionToggles.supervisionOneOnOneChats) &&
        Objects.equals(this.audioCalls, tenantAdminAllowedPermissionToggles.audioCalls) &&
        Objects.equals(this.audioCallsAnonymousChats, tenantAdminAllowedPermissionToggles.audioCallsAnonymousChats) &&
        Objects.equals(this.audioCallsOneOnOneChats, tenantAdminAllowedPermissionToggles.audioCallsOneOnOneChats) &&
        Objects.equals(this.audioCallsGroupChats, tenantAdminAllowedPermissionToggles.audioCallsGroupChats) &&
        Objects.equals(this.audioCallsSupervisionChats, tenantAdminAllowedPermissionToggles.audioCallsSupervisionChats) &&
        Objects.equals(this.videoCalls, tenantAdminAllowedPermissionToggles.videoCalls) &&
        Objects.equals(this.videoCallsAnonymousChats, tenantAdminAllowedPermissionToggles.videoCallsAnonymousChats) &&
        Objects.equals(this.videoCallsOneOnOneChats, tenantAdminAllowedPermissionToggles.videoCallsOneOnOneChats) &&
        Objects.equals(this.videoCallsGroupChats, tenantAdminAllowedPermissionToggles.videoCallsGroupChats) &&
        Objects.equals(this.videoCallsSupervisionChats, tenantAdminAllowedPermissionToggles.videoCallsSupervisionChats) &&
        Objects.equals(this.threads, tenantAdminAllowedPermissionToggles.threads) &&
        Objects.equals(this.threadsAnonymousChats, tenantAdminAllowedPermissionToggles.threadsAnonymousChats) &&
        Objects.equals(this.threadsOneOnOneChats, tenantAdminAllowedPermissionToggles.threadsOneOnOneChats) &&
        Objects.equals(this.threadsGroupChats, tenantAdminAllowedPermissionToggles.threadsGroupChats) &&
        Objects.equals(this.threadsSupervisionChats, tenantAdminAllowedPermissionToggles.threadsSupervisionChats) &&
        Objects.equals(this.voiceMessages, tenantAdminAllowedPermissionToggles.voiceMessages) &&
        Objects.equals(this.voiceMessagesAnonymousChats, tenantAdminAllowedPermissionToggles.voiceMessagesAnonymousChats) &&
        Objects.equals(this.voiceMessagesOneOnOneChats, tenantAdminAllowedPermissionToggles.voiceMessagesOneOnOneChats) &&
        Objects.equals(this.voiceMessagesGroupChats, tenantAdminAllowedPermissionToggles.voiceMessagesGroupChats) &&
        Objects.equals(this.voiceMessagesSupervisionChats, tenantAdminAllowedPermissionToggles.voiceMessagesSupervisionChats);
  }

  @Override
  public int hashCode() {
    return Objects.hash(anonymousChat, calls, supervision, supervisionAnonymousChats, supervisionOneOnOneChats, audioCalls, audioCallsAnonymousChats, audioCallsOneOnOneChats, audioCallsGroupChats, audioCallsSupervisionChats, videoCalls, videoCallsAnonymousChats, videoCallsOneOnOneChats, videoCallsGroupChats, videoCallsSupervisionChats, threads, threadsAnonymousChats, threadsOneOnOneChats, threadsGroupChats, threadsSupervisionChats, voiceMessages, voiceMessagesAnonymousChats, voiceMessagesOneOnOneChats, voiceMessagesGroupChats, voiceMessagesSupervisionChats);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TenantAdminAllowedPermissionToggles {\n");
    sb.append("    anonymousChat: ").append(toIndentedString(anonymousChat)).append("\n");
    sb.append("    calls: ").append(toIndentedString(calls)).append("\n");
    sb.append("    supervision: ").append(toIndentedString(supervision)).append("\n");
    sb.append("    supervisionAnonymousChats: ").append(toIndentedString(supervisionAnonymousChats)).append("\n");
    sb.append("    supervisionOneOnOneChats: ").append(toIndentedString(supervisionOneOnOneChats)).append("\n");
    sb.append("    audioCalls: ").append(toIndentedString(audioCalls)).append("\n");
    sb.append("    audioCallsAnonymousChats: ").append(toIndentedString(audioCallsAnonymousChats)).append("\n");
    sb.append("    audioCallsOneOnOneChats: ").append(toIndentedString(audioCallsOneOnOneChats)).append("\n");
    sb.append("    audioCallsGroupChats: ").append(toIndentedString(audioCallsGroupChats)).append("\n");
    sb.append("    audioCallsSupervisionChats: ").append(toIndentedString(audioCallsSupervisionChats)).append("\n");
    sb.append("    videoCalls: ").append(toIndentedString(videoCalls)).append("\n");
    sb.append("    videoCallsAnonymousChats: ").append(toIndentedString(videoCallsAnonymousChats)).append("\n");
    sb.append("    videoCallsOneOnOneChats: ").append(toIndentedString(videoCallsOneOnOneChats)).append("\n");
    sb.append("    videoCallsGroupChats: ").append(toIndentedString(videoCallsGroupChats)).append("\n");
    sb.append("    videoCallsSupervisionChats: ").append(toIndentedString(videoCallsSupervisionChats)).append("\n");
    sb.append("    threads: ").append(toIndentedString(threads)).append("\n");
    sb.append("    threadsAnonymousChats: ").append(toIndentedString(threadsAnonymousChats)).append("\n");
    sb.append("    threadsOneOnOneChats: ").append(toIndentedString(threadsOneOnOneChats)).append("\n");
    sb.append("    threadsGroupChats: ").append(toIndentedString(threadsGroupChats)).append("\n");
    sb.append("    threadsSupervisionChats: ").append(toIndentedString(threadsSupervisionChats)).append("\n");
    sb.append("    voiceMessages: ").append(toIndentedString(voiceMessages)).append("\n");
    sb.append("    voiceMessagesAnonymousChats: ").append(toIndentedString(voiceMessagesAnonymousChats)).append("\n");
    sb.append("    voiceMessagesOneOnOneChats: ").append(toIndentedString(voiceMessagesOneOnOneChats)).append("\n");
    sb.append("    voiceMessagesGroupChats: ").append(toIndentedString(voiceMessagesGroupChats)).append("\n");
    sb.append("    voiceMessagesSupervisionChats: ").append(toIndentedString(voiceMessagesSupervisionChats)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

