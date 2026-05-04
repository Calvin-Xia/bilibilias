package com.imcys.bilibilias.network.model.github

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName


@Serializable
data class GithubCommit(
    @SerialName("author")
    val author: Author,
    @SerialName("comments_url")
    val commentsUrl: String,
    @SerialName("commit")
    val commit: Commit,
    @SerialName("committer")
    val committer: Committer,
    @SerialName("html_url")
    val htmlUrl: String,
    @SerialName("node_id")
    val nodeId: String,
    @SerialName("parents")
    val parents: List<Parent>,
    @SerialName("sha")
    val sha: String,
    @SerialName("url")
    val url: String
) {
    @Serializable
    data class Author(
        @SerialName("avatar_url")
        val avatarUrl: String,
        @SerialName("events_url")
        val eventsUrl: String,
        @SerialName("followers_url")
        val followersUrl: String,
        @SerialName("following_url")
        val followingUrl: String,
        @SerialName("gists_url")
        val gistsUrl: String,
        @SerialName("gravatar_id")
        val gravatarId: String,
        @SerialName("html_url")
        val htmlUrl: String,
        @SerialName("id")
        val id: Int,
        @SerialName("login")
        val login: String,
        @SerialName("node_id")
        val nodeId: String,
        @SerialName("organizations_url")
        val organizationsUrl: String,
        @SerialName("received_events_url")
        val receivedEventsUrl: String,
        @SerialName("repos_url")
        val reposUrl: String,
        @SerialName("site_admin")
        val siteAdmin: Boolean,
        @SerialName("starred_url")
        val starredUrl: String,
        @SerialName("subscriptions_url")
        val subscriptionsUrl: String,
        @SerialName("type")
        val type: String,
        @SerialName("url")
        val url: String,
        @SerialName("user_view_type")
        val userViewType: String
    )

    @Serializable
    data class Commit(
        @SerialName("author")
        val author: Author,
        @SerialName("comment_count")
        val commentCount: Int,
        @SerialName("committer")
        val committer: Committer,
        @SerialName("message")
        val message: String,
        @SerialName("tree")
        val tree: Tree,
        @SerialName("url")
        val url: String,
        @SerialName("verification")
        val verification: Verification
    ) {
        @Serializable
        data class Author(
            @SerialName("date")
            val date: String,
            @SerialName("email")
            val email: String,
            @SerialName("name")
            val name: String
        )

        @Serializable
        data class Committer(
            @SerialName("date")
            val date: String,
            @SerialName("email")
            val email: String,
            @SerialName("name")
            val name: String
        )

        @Serializable
        data class Tree(
            @SerialName("sha")
            val sha: String,
            @SerialName("url")
            val url: String
        )

        @Serializable
        data class Verification(
            @SerialName("verified")
            val verified: Boolean,
        )
    }

    @Serializable
    data class Committer(
        @SerialName("avatar_url")
        val avatarUrl: String,
        @SerialName("events_url")
        val eventsUrl: String,
        @SerialName("followers_url")
        val followersUrl: String,
        @SerialName("following_url")
        val followingUrl: String,
        @SerialName("gists_url")
        val gistsUrl: String,
        @SerialName("gravatar_id")
        val gravatarId: String,
        @SerialName("html_url")
        val htmlUrl: String,
        @SerialName("id")
        val id: Int,
        @SerialName("login")
        val login: String,
        @SerialName("node_id")
        val nodeId: String,
        @SerialName("organizations_url")
        val organizationsUrl: String,
        @SerialName("received_events_url")
        val receivedEventsUrl: String,
        @SerialName("repos_url")
        val reposUrl: String,
        @SerialName("site_admin")
        val siteAdmin: Boolean,
        @SerialName("starred_url")
        val starredUrl: String,
        @SerialName("subscriptions_url")
        val subscriptionsUrl: String,
        @SerialName("type")
        val type: String,
        @SerialName("url")
        val url: String,
        @SerialName("user_view_type")
        val userViewType: String
    )

    @Serializable
    data class Parent(
        @SerialName("html_url")
        val htmlUrl: String,
        @SerialName("sha")
        val sha: String,
        @SerialName("url")
        val url: String
    )
}
