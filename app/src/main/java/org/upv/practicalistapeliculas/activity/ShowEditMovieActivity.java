package org.upv.practicalistapeliculas.activity;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.Toast;

import com.google.gson.Gson;

import org.upv.practicalistapeliculas.R;
import org.upv.practicalistapeliculas.model.User;
import org.upv.practicalistapeliculas.utils.DownloadImageTask;
import org.upv.practicalistapeliculas.model.Movie;
import org.upv.practicalistapeliculas.movie.MovieList;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static org.upv.practicalistapeliculas.activity.PerfilActivity.USERS;
import static org.upv.practicalistapeliculas.activity.PerfilActivity.USERS_KEY_USERS;
import static org.upv.practicalistapeliculas.activity.PerfilActivity.USER_LOGIN_PREFERENCES;
import static org.upv.practicalistapeliculas.activity.PerfilActivity.USER_LOGIN_PREFERENCES_KEY_USER;

/**
 * Created by jvg63 on 09/11/2017.
 */

public class ShowEditMovieActivity extends AppCompatActivity {
    public static String PARAM_EXTRA_ID_PELICULA = "ID";

    private ImageView photo;
    private EditText title, category, summary, directors, actors, producers, studio, comment;
    private RatingBar rating;
    private Button showComments, pushComment;
    private Float userRating;
    private Set<String> userList;
    //private SharedPreferences.Editor editor;
    //private SharedPreferences prefs;
    //Lista de valoraciones
    private Set movieRatings;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_edit_movie);

        photo = findViewById(R.id.photo);
        title = findViewById(R.id.title);
        category = findViewById(R.id.category);
        summary = findViewById(R.id.summary);
        actors = findViewById(R.id.actor);
        directors = findViewById(R.id.director);
        producers = findViewById(R.id.producer);
        studio = findViewById(R.id.studio);
        rating = findViewById(R.id.ratingBar);
        showComments = findViewById(R.id.buttonShowComments);
        pushComment = findViewById(R.id.buttonComment);
        comment = findViewById(R.id.comment);
        //btnSave = findViewById(R.id.fab);

        /*prefs = getSharedPreferences("Usuarios", Context.MODE_PRIVATE);
        editor = prefs.edit();
        userList = prefs.getStringSet("users", userList );*/

        Intent data = getIntent();
        int id = -1;

        if (data != null && data.getExtras() != null) {
            id = data.getExtras().getInt(PARAM_EXTRA_ID_PELICULA, -1);
        }

        if (id == -1) {
            // Mode Edit
        } else {
//            leerDatos();
            // Mode View
            postponeEnterTransition();
            mostrarPelicula(id);
        }


    }

    private void scheduleStartPostponedTransition(final View sharedElement) {
        sharedElement.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        sharedElement.getViewTreeObserver().removeOnPreDrawListener(this);
                        startPostponedEnterTransition();
                        return true;
                    }
                });
    }


    private void mostrarPelicula(final int id) {
        final Movie movie = MovieList.list.get(id);

        title.setText(movie.getTitle());
        category.setText(movie.getCategory());
        summary.setText(movie.getDescription());
        studio.setText(movie.getStudio());
        directors.setText(movie.getDirectors());
        actors.setText(movie.getActors());
        producers.setText(movie.getProducers());

        //Se comprueba si el usuario ha valorado
        final User user = readUserFromPreferences();

        String[] ratingComment = user.getRating(movie.getId()).split("-");
        Float userVal = Float.parseFloat(ratingComment[0]);
        rating.setRating(userVal);
        if (userVal == 0.0f) {
            rating.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
                @Override
                public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                    userRating = rating;
                }
            });

            pushComment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (userRating != null && comment.getText().length() != 0) {
                        user.setRating(id, userRating, comment.getText().toString());
                        writeUserToPreferences(user);
                        writeRatingToPreferences(id);
                        movie.addRating(userRating);
                        showAllComments(id);
                    } else {
                        Toast.makeText(getApplication(), R.string.ASEM_add_comment_rating, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            //rating.setEnabled(false);
            comment.setText(ratingComment[1]);
            //comment.setFocusable(false);
            //pushComment.setEnabled(false);
        }

        showComments.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAllComments(id);
            }
        });



        new DownloadImageTask(photo).execute(movie.getBackgroundImageUrl());
        protectFields();

        Transition lista_enter = TransitionInflater.from(this)
                .inflateTransition(R.transition.transition_curva);
        getWindow().setSharedElementEnterTransition(lista_enter);
        scheduleStartPostponedTransition(photo);


    }

    private void protectFields() {
        photo.setFocusable(false);
        title.setFocusable(false);
        studio.setFocusable(false);
        category.setFocusable(false);
        summary.setFocusable(false);
        actors.setFocusable(false);
        directors.setFocusable(false);
        producers.setFocusable(false);
        //rating.setEnabled(false);
        //btnSave.setVisibility(View.GONE);
    }

//    private void leerDatos() {
//        if (MovieList.list != null)
//            MovieList.list.clear();
//        else
//            MovieList.list = new ArrayList<>();
//
//        String json = Utils.loadJSONFromResource(this, R.raw.movies);
//        Gson gson = new Gson();
//        Type collection = new TypeToken<ArrayList<Movie>>() {
//        }.getType();
//        MovieList.list = gson.fromJson(json, collection);
//    }

    public User readUserFromPreferences() {
        User user = null;

        SharedPreferences prefsLogin = getSharedPreferences(USER_LOGIN_PREFERENCES, Context.MODE_PRIVATE);
        String userLogged = prefsLogin.getString(USER_LOGIN_PREFERENCES_KEY_USER, "");
        SharedPreferences prefs = getSharedPreferences(USERS, Context.MODE_PRIVATE);
        userList = prefs.getStringSet("users", userList );

        Gson gson = new Gson();

        for (String anUserList : userList) {
            User userAux = gson.fromJson(anUserList, User.class);
            if (userLogged.equals(userAux.getUsername())) {
                user = userAux;
                break;
            }
        }
        return user;
    }

    public void writeUserToPreferences(User user) {
        SharedPreferences prefs = getSharedPreferences("Usuarios", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(user);

        for(String anUserList : userList) {
            User userAux = gson.fromJson(anUserList, User.class);
            if(userAux.getUsername().equals(user.getUsername())) {
                if(userList.size() > 1) {
                    userList.remove(userAux);
                    userList.add(json);
                } else {
                    userList = new HashSet<>();
                    userList.add(json);
                }
            }
        }

        //editor = prefs.edit();
        editor.putStringSet(USERS_KEY_USERS, userList);
        editor.apply();
    }

    private void writeRatingToPreferences(int id) {
        SharedPreferences prefs = getSharedPreferences("Valoraciones", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        movieRatings = new HashSet<String>();
        movieRatings = prefs.getStringSet("ratings", movieRatings);
        Gson gson = new Gson();
        String json = gson.toJson(id + "-" + userRating);
        movieRatings.add(json);
        editor.putStringSet("ratings", movieRatings);
        editor.apply();
    }

    private void showAllComments(int id) {
        Intent intent = new Intent(getApplication(), Ratings.class);
        intent.putExtra("idPelicula", id);
        startActivity(intent);
    }
}
