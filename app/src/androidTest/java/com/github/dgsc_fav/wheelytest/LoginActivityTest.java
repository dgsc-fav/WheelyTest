package com.github.dgsc_fav.wheelytest;

import android.support.annotation.Nullable;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.widget.EditText;

import com.github.dgsc_fav.wheelytest.ui.activity.LoginActivity;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

/**
 * Created by DG on 19.10.2016.
 */
@RunWith(AndroidJUnit4.class)
public class LoginActivityTest {

    private String mValidString;
    private String mInvalidString;
    private String mUsernameError;
    private String mPasswordError;

    @Rule
    public ActivityTestRule<LoginActivity> mActivityRule = new ActivityTestRule<>(LoginActivity.class);

    @Before
    public void initValidString() {
        mValidString = "NonnullString";
        mInvalidString = ""; // empty String

        mUsernameError = mActivityRule.getActivity().getString(R.string.username_error);
        mPasswordError = mActivityRule.getActivity().getString(R.string.password_error);
    }

    @Test
    public void changeUsernameText_errorActivity() {
        onView(withId(R.id.username)).check(matches(withText("")));
        onView(withId(R.id.password)).check(matches(withText("")));

        // Invalid username
        onView(withId(R.id.username)).perform(typeText(mInvalidString), closeSoftKeyboard());
        // Invalid password
        onView(withId(R.id.password)).perform(typeText(mInvalidString), closeSoftKeyboard());

        onView(withId(R.id.connect)).perform(click());

        // ожидание ошибки в поле ввода username
        onView(withId(R.id.username)).check(matches(withError(mUsernameError)));
    }

    @Test
    public void changePasswordText_errorActivity() {
        onView(withId(R.id.username)).check(matches(withText("")));
        onView(withId(R.id.password)).check(matches(withText("")));

        // Valid username
        onView(withId(R.id.username)).perform(typeText(mValidString), closeSoftKeyboard());
        // Invalid password
        onView(withId(R.id.password)).perform(typeText(mInvalidString), closeSoftKeyboard());

        onView(withId(R.id.connect)).perform(click());

        // ошибки в поле ввода username быть не должно
        onView(withId(R.id.username)).check(matches(withError(null)));
        // ожидание ошибки в поле ввода password
        onView(withId(R.id.password)).check(matches(withError(mPasswordError)));
    }

    @Test
    public void changeUsernamePasswordText_noErrorActivity() {
        onView(withId(R.id.username)).check(matches(withText("")));
        onView(withId(R.id.password)).check(matches(withText("")));

        // Valid username
        onView(withId(R.id.username)).perform(typeText(mValidString), closeSoftKeyboard());
        // Valid password
        onView(withId(R.id.password)).perform(typeText(mValidString), closeSoftKeyboard());

        onView(withId(R.id.connect)).perform(click());

        // ошибки в поле ввода username быть не должно
        onView(withId(R.id.username)).check(matches(withError(null)));
        // ошибки в поле ввода password быть не должно
        onView(withId(R.id.password)).check(matches(withError(null)));
    }

    private static Matcher<View> withError(@Nullable final CharSequence expectedError) {
        return new TypeSafeMatcher<View>() {

            @Override
            public boolean matchesSafely(View view) {
                if(!(view instanceof EditText)) {
                    return false;
                }

                CharSequence error = ((EditText) view).getError();

                return expectedError == null && error == null || expectedError != null && expectedError.equals(error);
            }

            @Override
            public void describeTo(Description description) {
            }
        };
    }
}
