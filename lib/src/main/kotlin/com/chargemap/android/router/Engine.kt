package com.chargemap.android.router

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.chargemap.android.router.destinations.Route
import com.chargemap.android.router.destinations.RouteWithInit
import com.chargemap.android.router.destinations.RouteWithParam
import com.chargemap.android.router.destinations.RouteWithParamAndInit

sealed class RouteEngine {
    abstract val context: Context
    abstract val activity: Activity?

    protected abstract fun start(intent: Intent)
    protected abstract fun startForResult(intent: Intent, code: Int)

    fun <T : Route> push(
        route: T,
        finishActivity: Boolean = false
    ) =
        push(
            intent = Router.getIntent(context, route),
            requestCode = route.requestCode,
            finishActivity = finishActivity
        )

    fun <I : RouteInit, T : RouteWithInit<I>> push(
        route: T,
        init: I,
        finishActivity: Boolean = false
    ) =
        push(
            intent = Router.getIntent(context, route, init),
            requestCode = route.requestCode,
            finishActivity = finishActivity
        )

    fun <P : RouteParam, T : RouteWithParam<P>> push(
        route: T,
        param: P,
        finishActivity: Boolean = false
    ) =
        push(
            intent = Router.getIntent(context, route, param),
            requestCode = route.requestCode,
            finishActivity = finishActivity
        )

    fun <P : RouteParam, I : RouteInit, T : RouteWithParamAndInit<P, I>> push(
        route: T,
        param: P,
        init: I,
        finishActivity: Boolean = false
    ) {
        push(
            intent = Router.getIntent(context, route, param, init),
            requestCode = route.requestCode,
            finishActivity = finishActivity
        )
    }

    private fun push(
        intent: Intent,
        requestCode: Int? = null,
        finishActivity: Boolean = false
    ) {
        if (requestCode == null) {
            start(intent)
        } else {
            startForResult(intent, requestCode)
        }

        if (finishActivity) {
            activity?.finish()
        }
    }

    fun push(path: String) {
        //try to find by name
        Router.routes.keys
            .firstOrNull {
                it.path == path && it is Route
            }?.let {
                push(it as Route)
            }

        //try to find by regex
        for ((destination, routing) in Router.routes) {
            if (destination.pathMatcher.matches(url = path)) {

                // We found a path matching the pattern
                val parametersValues = destination.pathMatcher.parametersValues(url = path)

                push(
                    intent = routing.creator(context)
                        .putExtra("bundle", Bundle().apply {
                            routing.bundleCreator?.let { map ->
                                putParcelable("routeParam", map(parametersValues))
                            }
                        }),
                    requestCode = null,
                    finishActivity = false
                )

                break
            }
        }
    }

    private fun pushBottomSheet(fragment: DialogFragment, path: String) {
        (activity as? FragmentActivity)?.let {
            fragment.show(it.supportFragmentManager, path)
        }
    }

    fun <T : Route> pushBottomSheet(route: T) {
        pushBottomSheet(Router.getFragment(route) as DialogFragment, route.path)
    }

    fun <P : RouteParam, T : RouteWithParam<P>> pushBottomSheet(
        route: T,
        param: P
    ) {
        pushBottomSheet(Router.getFragment(route, param) as DialogFragment, route.path)
    }

    fun <I : RouteInit, T : RouteWithInit<I>> pushBottomSheet(
        route: T,
        init: I,
    ) {
        pushBottomSheet(Router.getFragment(route, init) as DialogFragment, route.path)
    }

    fun <P : RouteParam, I : RouteInit, T : RouteWithParamAndInit<P, I>> pushBottomSheet(
        route: T,
        param: P,
        init: I
    ) {
        val bs = Router.getFragment(route, param, init) as DialogFragment
        (activity as? FragmentActivity)?.let {
            bs.show(it.supportFragmentManager, route.path)
        }
    }
}

class ActivityEngine(override val activity: Activity) : RouteEngine() {

    override val context get() = activity

    override fun start(intent: Intent) {
        activity.startActivity(intent)
    }

    override fun startForResult(intent: Intent, code: Int) {
        activity.startActivityForResult(intent, code)
    }
}

class FragmentEngine(private val fragment: Fragment) : RouteEngine() {

    override val context get() = fragment.context ?: throw Exception("no context to push the route")
    override val activity: Activity? = fragment.activity

    override fun start(intent: Intent) {
        fragment.startActivity(intent)
    }

    override fun startForResult(intent: Intent, code: Int) {
        fragment.startActivityForResult(intent, code)
    }
}
