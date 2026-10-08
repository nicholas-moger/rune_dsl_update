package chaos.s17.a2dangle;

import chaos.s17.a2dangle.h.C17Held;
import chaos.s17.a2dangle.meta.C17BooksMeta;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * Both colliding enums at attribute seats.
 * @version 1.0.0
 */
@RosettaDataType(value="C17Books", builder=C17Books.C17BooksBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C17Books", model="chaos", builder=C17Books.C17BooksBuilderImpl.class, version="1.0.0")
public interface C17Books extends RosettaModelObject {

	C17BooksMeta metaData = new C17BooksMeta();

	/*********************** Getter Methods  ***********************/
	C17SideEnum getSide();
	C17ActionEnum getAction();
	C17Held getHeld();

	/*********************** Build Methods  ***********************/
	C17Books build();
	
	C17Books.C17BooksBuilder toBuilder();
	
	static C17Books.C17BooksBuilder builder() {
		return new C17Books.C17BooksBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C17Books> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C17Books> getType() {
		return C17Books.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("side"), C17SideEnum.class, getSide(), this);
		processor.processBasic(path.newSubPath("action"), C17ActionEnum.class, getAction(), this);
		processRosetta(path.newSubPath("held"), processor, C17Held.class, getHeld());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C17BooksBuilder extends C17Books, RosettaModelObjectBuilder {
		C17Held.C17HeldBuilder getOrCreateHeld();
		@Override
		C17Held.C17HeldBuilder getHeld();
		C17Books.C17BooksBuilder setSide(C17SideEnum side);
		C17Books.C17BooksBuilder setAction(C17ActionEnum action);
		C17Books.C17BooksBuilder setHeld(C17Held held);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("side"), C17SideEnum.class, getSide(), this);
			processor.processBasic(path.newSubPath("action"), C17ActionEnum.class, getAction(), this);
			processRosetta(path.newSubPath("held"), processor, C17Held.C17HeldBuilder.class, getHeld());
		}
		

		C17Books.C17BooksBuilder prune();
	}

	/*********************** Immutable Implementation of C17Books  ***********************/
	class C17BooksImpl implements C17Books {
		private final C17SideEnum side;
		private final C17ActionEnum action;
		private final C17Held held;
		
		protected C17BooksImpl(C17Books.C17BooksBuilder builder) {
			this.side = builder.getSide();
			this.action = builder.getAction();
			this.held = ofNullable(builder.getHeld()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("side")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("side")
		public C17SideEnum getSide() {
			return side;
		}
		
		@Override
		@RosettaAttribute("action")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("action")
		public C17ActionEnum getAction() {
			return action;
		}
		
		@Override
		@RosettaAttribute("held")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("held")
		public C17Held getHeld() {
			return held;
		}
		
		@Override
		public C17Books build() {
			return this;
		}
		
		@Override
		public C17Books.C17BooksBuilder toBuilder() {
			C17Books.C17BooksBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C17Books.C17BooksBuilder builder) {
			ofNullable(getSide()).ifPresent(builder::setSide);
			ofNullable(getAction()).ifPresent(builder::setAction);
			ofNullable(getHeld()).ifPresent(builder::setHeld);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C17Books _that = getType().cast(o);
		
			if (!Objects.equals(side, _that.getSide())) return false;
			if (!Objects.equals(action, _that.getAction())) return false;
			if (!Objects.equals(held, _that.getHeld())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (side != null ? side.getClass().getName().hashCode() : 0);
			_result = 31 * _result + (action != null ? action.getClass().getName().hashCode() : 0);
			_result = 31 * _result + (held != null ? held.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C17Books {" +
				"side=" + this.side + ", " +
				"action=" + this.action + ", " +
				"held=" + this.held +
			'}';
		}
	}

	/*********************** Builder Implementation of C17Books  ***********************/
	class C17BooksBuilderImpl implements C17Books.C17BooksBuilder {
	
		protected C17SideEnum side;
		protected C17ActionEnum action;
		protected C17Held.C17HeldBuilder held;
		
		@Override
		@RosettaAttribute("side")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("side")
		public C17SideEnum getSide() {
			return side;
		}
		
		@Override
		@RosettaAttribute("action")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("action")
		public C17ActionEnum getAction() {
			return action;
		}
		
		@Override
		@RosettaAttribute("held")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("held")
		public C17Held.C17HeldBuilder getHeld() {
			return held;
		}
		
		@Override
		public C17Held.C17HeldBuilder getOrCreateHeld() {
			C17Held.C17HeldBuilder result;
			if (held!=null) {
				result = held;
			}
			else {
				result = held = C17Held.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("side")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("side")
		@Override
		public C17Books.C17BooksBuilder setSide(C17SideEnum _side) {
			this.side = _side == null ? null : _side;
			return this;
		}
		
		@RosettaAttribute("action")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("action")
		@Override
		public C17Books.C17BooksBuilder setAction(C17ActionEnum _action) {
			this.action = _action == null ? null : _action;
			return this;
		}
		
		@RosettaAttribute("held")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("held")
		@Override
		public C17Books.C17BooksBuilder setHeld(C17Held _held) {
			this.held = _held == null ? null : _held.toBuilder();
			return this;
		}
		
		@Override
		public C17Books build() {
			return new C17Books.C17BooksImpl(this);
		}
		
		@Override
		public C17Books.C17BooksBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C17Books.C17BooksBuilder prune() {
			if (held!=null && !held.prune().hasData()) held = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getSide()!=null) return true;
			if (getAction()!=null) return true;
			if (getHeld()!=null && getHeld().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C17Books.C17BooksBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C17Books.C17BooksBuilder o = (C17Books.C17BooksBuilder) other;
			
			merger.mergeRosetta(getHeld(), o.getHeld(), this::setHeld);
			
			merger.mergeBasic(getSide(), o.getSide(), this::setSide);
			merger.mergeBasic(getAction(), o.getAction(), this::setAction);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C17Books _that = getType().cast(o);
		
			if (!Objects.equals(side, _that.getSide())) return false;
			if (!Objects.equals(action, _that.getAction())) return false;
			if (!Objects.equals(held, _that.getHeld())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (side != null ? side.getClass().getName().hashCode() : 0);
			_result = 31 * _result + (action != null ? action.getClass().getName().hashCode() : 0);
			_result = 31 * _result + (held != null ? held.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C17BooksBuilder {" +
				"side=" + this.side + ", " +
				"action=" + this.action + ", " +
				"held=" + this.held +
			'}';
		}
	}
}
