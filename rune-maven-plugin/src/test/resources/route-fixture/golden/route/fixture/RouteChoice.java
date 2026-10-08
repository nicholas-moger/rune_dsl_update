package route.fixture;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneChoiceType;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.util.Objects;
import route.fixture.meta.RouteChoiceMeta;

import static java.util.Optional.ofNullable;

/**
 * the CHOICE pass&#39;s population.
 * @version 1.0.0
 */
@RosettaDataType(value="RouteChoice", builder=RouteChoice.RouteChoiceBuilderImpl.class, version="1.0.0")
@RuneDataType(value="RouteChoice", model="route", builder=RouteChoice.RouteChoiceBuilderImpl.class, version="1.0.0")
@RuneChoiceType
public interface RouteChoice extends RosettaModelObject {

	RouteChoiceMeta metaData = new RouteChoiceMeta();

	/*********************** Getter Methods  ***********************/
	/**
	 * the extending data type.
	 */
	RouteChild getRouteChild();
	/**
	 * the deep-path eligible data type.
	 */
	RouteDeep getRouteDeep();

	/*********************** Build Methods  ***********************/
	RouteChoice build();
	
	RouteChoice.RouteChoiceBuilder toBuilder();
	
	static RouteChoice.RouteChoiceBuilder builder() {
		return new RouteChoice.RouteChoiceBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends RouteChoice> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends RouteChoice> getType() {
		return RouteChoice.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("RouteChild"), processor, RouteChild.class, getRouteChild());
		processRosetta(path.newSubPath("RouteDeep"), processor, RouteDeep.class, getRouteDeep());
	}
	

	/*********************** Builder Interface  ***********************/
	interface RouteChoiceBuilder extends RouteChoice, RosettaModelObjectBuilder {
		RouteChild.RouteChildBuilder getOrCreateRouteChild();
		@Override
		RouteChild.RouteChildBuilder getRouteChild();
		RouteDeep.RouteDeepBuilder getOrCreateRouteDeep();
		@Override
		RouteDeep.RouteDeepBuilder getRouteDeep();
		RouteChoice.RouteChoiceBuilder setRouteChild(RouteChild _RouteChild);
		RouteChoice.RouteChoiceBuilder setRouteDeep(RouteDeep _RouteDeep);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("RouteChild"), processor, RouteChild.RouteChildBuilder.class, getRouteChild());
			processRosetta(path.newSubPath("RouteDeep"), processor, RouteDeep.RouteDeepBuilder.class, getRouteDeep());
		}
		

		RouteChoice.RouteChoiceBuilder prune();
	}

	/*********************** Immutable Implementation of RouteChoice  ***********************/
	class RouteChoiceImpl implements RouteChoice {
		private final RouteChild routeChild;
		private final RouteDeep routeDeep;
		
		protected RouteChoiceImpl(RouteChoice.RouteChoiceBuilder builder) {
			this.routeChild = ofNullable(builder.getRouteChild()).map(f->f.build()).orElse(null);
			this.routeDeep = ofNullable(builder.getRouteDeep()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("RouteChild")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("RouteChild")
		public RouteChild getRouteChild() {
			return routeChild;
		}
		
		@Override
		@RosettaAttribute("RouteDeep")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("RouteDeep")
		public RouteDeep getRouteDeep() {
			return routeDeep;
		}
		
		@Override
		public RouteChoice build() {
			return this;
		}
		
		@Override
		public RouteChoice.RouteChoiceBuilder toBuilder() {
			RouteChoice.RouteChoiceBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(RouteChoice.RouteChoiceBuilder builder) {
			ofNullable(getRouteChild()).ifPresent(builder::setRouteChild);
			ofNullable(getRouteDeep()).ifPresent(builder::setRouteDeep);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RouteChoice _that = getType().cast(o);
		
			if (!Objects.equals(routeChild, _that.getRouteChild())) return false;
			if (!Objects.equals(routeDeep, _that.getRouteDeep())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (routeChild != null ? routeChild.hashCode() : 0);
			_result = 31 * _result + (routeDeep != null ? routeDeep.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "RouteChoice {" +
				"RouteChild=" + this.routeChild + ", " +
				"RouteDeep=" + this.routeDeep +
			'}';
		}
	}

	/*********************** Builder Implementation of RouteChoice  ***********************/
	class RouteChoiceBuilderImpl implements RouteChoice.RouteChoiceBuilder {
	
		protected RouteChild.RouteChildBuilder routeChild;
		protected RouteDeep.RouteDeepBuilder routeDeep;
		
		@Override
		@RosettaAttribute("RouteChild")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("RouteChild")
		public RouteChild.RouteChildBuilder getRouteChild() {
			return routeChild;
		}
		
		@Override
		public RouteChild.RouteChildBuilder getOrCreateRouteChild() {
			RouteChild.RouteChildBuilder result;
			if (routeChild!=null) {
				result = routeChild;
			}
			else {
				result = routeChild = RouteChild.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("RouteDeep")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("RouteDeep")
		public RouteDeep.RouteDeepBuilder getRouteDeep() {
			return routeDeep;
		}
		
		@Override
		public RouteDeep.RouteDeepBuilder getOrCreateRouteDeep() {
			RouteDeep.RouteDeepBuilder result;
			if (routeDeep!=null) {
				result = routeDeep;
			}
			else {
				result = routeDeep = RouteDeep.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("RouteChild")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("RouteChild")
		@Override
		public RouteChoice.RouteChoiceBuilder setRouteChild(RouteChild _routeChild) {
			this.routeChild = _routeChild == null ? null : _routeChild.toBuilder();
			return this;
		}
		
		@RosettaAttribute("RouteDeep")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("RouteDeep")
		@Override
		public RouteChoice.RouteChoiceBuilder setRouteDeep(RouteDeep _routeDeep) {
			this.routeDeep = _routeDeep == null ? null : _routeDeep.toBuilder();
			return this;
		}
		
		@Override
		public RouteChoice build() {
			return new RouteChoice.RouteChoiceImpl(this);
		}
		
		@Override
		public RouteChoice.RouteChoiceBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RouteChoice.RouteChoiceBuilder prune() {
			if (routeChild!=null && !routeChild.prune().hasData()) routeChild = null;
			if (routeDeep!=null && !routeDeep.prune().hasData()) routeDeep = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getRouteChild()!=null && getRouteChild().hasData()) return true;
			if (getRouteDeep()!=null && getRouteDeep().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RouteChoice.RouteChoiceBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			RouteChoice.RouteChoiceBuilder o = (RouteChoice.RouteChoiceBuilder) other;
			
			merger.mergeRosetta(getRouteChild(), o.getRouteChild(), this::setRouteChild);
			merger.mergeRosetta(getRouteDeep(), o.getRouteDeep(), this::setRouteDeep);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RouteChoice _that = getType().cast(o);
		
			if (!Objects.equals(routeChild, _that.getRouteChild())) return false;
			if (!Objects.equals(routeDeep, _that.getRouteDeep())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (routeChild != null ? routeChild.hashCode() : 0);
			_result = 31 * _result + (routeDeep != null ? routeDeep.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "RouteChoiceBuilder {" +
				"RouteChild=" + this.routeChild + ", " +
				"RouteDeep=" + this.routeDeep +
			'}';
		}
	}
}
