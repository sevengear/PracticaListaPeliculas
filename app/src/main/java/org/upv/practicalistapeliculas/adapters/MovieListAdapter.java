package org.upv.practicalistapeliculas.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.upv.practicalistapeliculas.R;
import org.upv.practicalistapeliculas.model.Movie;

import java.util.List;

/**
 * Created by Miguel Á. Núñez on 11/11/2017.
 */

public class MovieListAdapter extends RecyclerView.Adapter<MovieListAdapter.MovieListViewHolder> {

    private List<Movie> items;

    public MovieListAdapter(List<Movie> items) {
        this.items = items;
    }

    @Override
    public MovieListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.pelicula_row, parent, false);
        return new MovieListViewHolder(v);
    }

    @Override
    public void onBindViewHolder(MovieListViewHolder holder, int position) {
        holder.title.setText(items.get(position).getTitle());
        holder.category.setText(items.get(position).getCategory());
        //Descargamos la imagen y se la añadimos al ImageView
        new DownloadImageTask(holder.poster).execute(items.get(position).getCardImageUrl());

    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class MovieListViewHolder extends RecyclerView.ViewHolder {

        private ImageView poster;
        private TextView title;
        private TextView category;

        private MovieListViewHolder(View v) {
            super(v);
            poster = v.findViewById(R.id.movie_poster);
            title = v.findViewById(R.id.movie_title);
            category = v.findViewById(R.id.movie_year);
        }
    }
}