package org.upv.practicalistapeliculas;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Lionel on 07/11/2017.
 */

public class ListaAdapter extends RecyclerView.Adapter <ListaAdapter.ListaViewHolder>{

    private ListasVector listasVector;

    public ListaAdapter(ListasVector listasVector) {
        this.listasVector = listasVector;
    }

    @Override public ListaViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {

        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.elemento_lista, viewGroup, false);
        return new ListaViewHolder(v);
    }

    @Override public void onBindViewHolder(ListaViewHolder viewHolder, int i) {

        viewHolder.imagen.setImageResource(listasVector.elemento(i).getImagen());
        viewHolder.titulo.setText(listasVector.elemento(i).getTitulo());
        viewHolder.descripcion.setText(listasVector.elemento(i).getDescripcion());
    }

    @Override public int getItemCount() {
        return listasVector.tamanyo();
    }

    public static class ListaViewHolder extends RecyclerView.ViewHolder {

        // Campos respectivos de un item
        public ImageView imagen;
        public TextView titulo;
        public TextView descripcion;

        public ListaViewHolder(View v) {
            super(v);
            imagen = (ImageView) v.findViewById(R.id.imagen);
            titulo = (TextView) v.findViewById(R.id.titulo);
            descripcion = (TextView) v.findViewById(R.id.descripcion);
        }
    }
}