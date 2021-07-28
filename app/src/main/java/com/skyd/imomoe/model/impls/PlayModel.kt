package com.skyd.imomoe.model.impls

import com.skyd.imomoe.bean.*
import com.skyd.imomoe.config.Api
import com.skyd.imomoe.config.Const
import com.skyd.imomoe.model.JsoupUtil
import com.skyd.imomoe.model.ParseHtmlUtil
import com.skyd.imomoe.model.interfaces.IPlayModel
import com.skyd.imomoe.model.util.Triple
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.util.ArrayList

class PlayModel : IPlayModel {
    private fun getVideoRawUrl(e: Element): String {
        val div = e.select("[class=area]").select("[class=bofang]")[0].children()
        val rawUrl = div.attr("data-vid")
        return when {
            rawUrl.endsWith("\$mp4", true) -> rawUrl.replace("\$mp4", "")
            rawUrl.endsWith("\$url", true) -> rawUrl.replace("\$url", "")
            rawUrl.endsWith("\$qzz", true) -> rawUrl
            else -> ""
        }
    }

    override fun getPlayData(
        partUrl: String?,
        animeEpisodeDataBean: AnimeEpisodeDataBean
    ): Triple<ArrayList<IAnimeDetailBean>, ArrayList<AnimeEpisodeDataBean>, PlayBean> {
        val playBeanDataList: ArrayList<IAnimeDetailBean> = ArrayList()
        val episodesList: ArrayList<AnimeEpisodeDataBean> = ArrayList()
        val title = AnimeTitleBean("", "", "")
        val episode =
            AnimeEpisodeDataBean(
                "", "",
                ""
            )
        val url = Api.MAIN_URL + partUrl
        val document = JsoupUtil.getDocument(url)
        val children: Elements = document.allElements
        for (i in children.indices) {
            when (children[i].className()) {
                "play" -> {
                    animeEpisodeDataBean.videoUrl = getVideoRawUrl(children[i])
                }
                "area" -> {
                    val areaChildren = children[i].children()
                    for (j in areaChildren.indices) {
                        when (areaChildren[j].className()) {
                            "gohome l" -> {        //标题
                                title.title = areaChildren[j].select("h1")
                                    .select("a").text()
                                title.actionUrl = areaChildren[j].select("h1")
                                    .select("a").attr("href")
                                episode.title = areaChildren[j].select("h1")
                                    .select("span").text().replace("：", "")
                            }
                            "botit" -> {
                                playBeanDataList.add(
                                    AnimeDetailBean(
                                        Const.ViewHolderTypeString.HEADER_1,
                                        "",
                                        ParseHtmlUtil.parseBotit(areaChildren[j]),
                                        ""
                                    )
                                )
                            }
                            "movurls" -> {      //集数列表
                                episodesList.addAll(
                                    ParseHtmlUtil.parseMovurls(
                                        areaChildren[j],
                                        animeEpisodeDataBean
                                    )
                                )
                                playBeanDataList.add(
                                    AnimeDetailBean(
                                        Const.ViewHolderTypeString.HORIZONTAL_RECYCLER_VIEW_1,
                                        "",
                                        "",
                                        "",
                                        episodesList
                                    )
                                )
                            }
                            "imgs" -> {
                                playBeanDataList.addAll(
                                    ParseHtmlUtil.parseImg(areaChildren[j], url)
                                )
                            }
                        }
                    }
                }
            }
        }
        val playBean = PlayBean("", "", title, episode, playBeanDataList)
        return Triple(playBeanDataList, episodesList, playBean)
    }

    override fun refreshAnimeEpisodeData(
        partUrl: String,
        animeEpisodeDataBean: AnimeEpisodeDataBean
    ): Boolean {
        val document = JsoupUtil.getDocument(Api.MAIN_URL + partUrl)
        val children: Elements = document.select("body")[0].children()
        for (i in children.indices) {
            when (children[i].className()) {
                "play" -> {
                    animeEpisodeDataBean.actionUrl = partUrl
                    animeEpisodeDataBean.videoUrl = getVideoRawUrl(children[i])
                    return true
                }
            }
        }
        return false
    }

    override fun getAnimeCoverImageBean(detailPartUrl: String): ImageBean? {
        try {
            val url = Api.MAIN_URL + detailPartUrl
            val document = JsoupUtil.getDocument(url)
            //番剧头部信息
            val area: Elements = document.getElementsByClass("area")
            for (i in area.indices) {
                val areaChildren = area[i].children()
                for (j in areaChildren.indices) {
                    when (areaChildren[j].className()) {
                        "fire l" -> {
                            val fireLChildren =
                                areaChildren[j].select("[class=fire l]")[0].children()
                            for (k in fireLChildren.indices) {
                                if (fireLChildren[k].className() == "thumb l") {
                                    return ImageBean(
                                        "", "", fireLChildren[k]
                                            .select("img").attr("src"), url
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    override fun getAnimeEpisodeUrlData(partUrl: String?): String? {
        val document = JsoupUtil.getDocument(Api.MAIN_URL + partUrl)
        val children: Elements = document.select("body")[0].children()
        for (i in children.indices) {
            when (children[i].className()) {
                "play" -> {
                    return getVideoRawUrl(children[i])
                }
            }
        }
        return null
    }

}