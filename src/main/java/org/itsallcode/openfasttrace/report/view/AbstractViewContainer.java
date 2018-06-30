package org.itsallcode.openfasttrace.report.view;

/*-
 * #%L
 * OpenFastTrace
 * %%
 * Copyright (C) 2016 - 2018 itsallcode.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for all containers of viewable elements
 */
public abstract class AbstractViewContainer implements ViewableContainer
{
    protected List<Viewable> children;

    /**
     * Create a new instance of type {@link AbstractViewContainer}.
     */
    public AbstractViewContainer()
    {
        this.children = new ArrayList<>();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.itsallcode.openfasttrace.report.view.Viewable#render(int)
     */
    @Override
    public void render(final int level)
    {
        renderBeforeChildren(level);
        renderChildren(level);
        renderAfterChildren(level);
    }

    /**
     * Render a the part of the view that comes before the children.
     * 
     * @param level
     *            indentation level
     */
    protected abstract void renderBeforeChildren(final int level);

    /**
     * Render a the children of this sub(view).
     * 
     * @param level
     *            indentation level
     */
    protected void renderChildren(final int level)
    {
        for (final Viewable child : this.children)
        {
            child.render(level + 1);
        }
    }

    /**
     * Render a the part of the view that comes after the children.
     * 
     * @param level
     *            indentation level
     */
    protected abstract void renderAfterChildren(final int level);

    /*
     * (non-Javadoc)
     * 
     * @see org.itsallcode.openfasttrace.report.view.ViewableContainer#add(org.
     * itsallcode. openfasttrace.view.Viewable)
     */
    @Override
    public void add(final Viewable child)
    {
        this.children.add(child);
    }
}
